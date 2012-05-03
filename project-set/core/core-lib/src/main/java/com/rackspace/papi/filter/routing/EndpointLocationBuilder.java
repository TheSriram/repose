package com.rackspace.papi.filter.routing;

import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.Destination;
import com.rackspace.papi.model.DomainNode;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class EndpointLocationBuilder implements LocationBuilder {

   private final Destination destination;
   private final List<Port> localPorts = new ArrayList<Port>();
   private final DomainNode localhost;
   private final String uri;
   private final HttpServletRequest request;

   public EndpointLocationBuilder(DomainNode localhost, Destination destination, String uri, HttpServletRequest request) {
      this.destination = destination;
      this.localhost = localhost;
      this.uri = uri;
      this.request = request;
      determineLocalPortsList();
   }

   @Override
   public DestinationLocation build() throws MalformedURLException, URISyntaxException {
      return new DestinationLocation(
              new EndpointUrlBuilder(localhost, localPorts, destination, uri, request).build(),
              new EndpointUriBuilder(localhost, localPorts, destination, uri).build());
   }

   private void determineLocalPortsList() {

      if (localhost.getHttpPort() > 0) {
         localPorts.add(new Port("http", localhost.getHttpPort()));
      }

      if (localhost.getHttpsPort() > 0) {
         localPorts.add(new Port("https", localhost.getHttpsPort()));
      }

   }
}