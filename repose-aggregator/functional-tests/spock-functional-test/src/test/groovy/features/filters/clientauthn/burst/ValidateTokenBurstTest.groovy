package features.filters.clientauthn.burst

import framework.ReposeValveTest
import framework.mocks.MockIdentityService
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Request
import org.rackspace.deproxy.Response

class ValidateTokenBurstTest extends ReposeValveTest {

    def static originEndpoint
    def static identityEndpoint
    static MockIdentityService fakeIdentityService

    def setupSpec() {
        deproxy = new Deproxy()


        repose.configurationProvider.applyConfigs("common", properties.defaultTemplateParams)
        repose.configurationProvider.applyConfigs("features/filters/clientauthn/common", properties.defaultTemplateParams)
        repose.start()

        originEndpoint = deproxy.addEndpoint(properties.targetPort, 'origin service')
        fakeIdentityService = new MockIdentityService(properties.identityPort, properties.targetPort)
        identityEndpoint = deproxy.addEndpoint(properties.identityPort,
                'identity service', null, fakeIdentityService.handler)

        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        Map acceptXML = ["accept": "application/xml"]

        def missingResponseErrorHandler = { Request request ->
            def headers = request.getHeaders()

            if (!headers.contains("X-Auth-Token") ) {
                return new Response(500, "INTERNAL SERVER ERROR", null, "MISSING AUTH TOKEN")
            }
            return new Response(200, "OK",header1+acceptXML)

        }

        deproxy.defaultHandler = missingResponseErrorHandler
    }


    def cleanupSpec() {
        if (deproxy) {
            deproxy.shutdown()
        }
        repose.stop()
    }





    def "under heavy load should not drop validate token response"() {

        given:
        Map header1 = ['X-Auth-Token': fakeIdentityService.client_token]
        fakeIdentityService.resetCounts()

        List<Thread> clientThreads = new ArrayList<Thread>()

        DateTimeFormatter fmt = DateTimeFormat
                .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                .withLocale(Locale.US)
                .withZone(DateTimeZone.UTC);

        def missingAuthResponse = false
        def missingAuthHeader = false
        (1..numClients).each {
            threadNum ->
            def thread = Thread.start {
                (1..callsPerClient).each {
                    def messageChain = deproxy.makeRequest(url: reposeEndpoint, method: 'GET', headers: header1)

                    if (messageChain.receivedResponse.code.equalsIgnoreCase("500")) {
                        missingAuthResponse = true
                    }
                    def sentToOrigin = ((MessageChain) messageChain).getHandlings()[0]

                    if (sentToOrigin.request.headers.findAll("x-roles").empty) {
                        missingAuthHeader = true
                    }

                }
            }
            clientThreads.add(thread)
        }

        when:
        clientThreads*.join()

        then:
        fakeIdentityService.validateTokenCount == 1

        and:
        fakeIdentityService.getGroupsCount == 1

        and:
        missingAuthHeader == false

        and:
        missingAuthResponse == false

        where:
        numClients | callsPerClient
        10         | 5

    }

}
