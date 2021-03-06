package com.rackspace.papi.components.datastore.impl.distributed.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.components.datastore.DatastoreOperationException;
import com.rackspace.papi.commons.util.proxy.ProxyRequestException;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;

import java.io.IOException;

public class RemoteCommandExecutor {

    private String hostKey;
    private final RequestProxyService proxyService;

    public RemoteCommandExecutor(RequestProxyService proxyService, String hostKey) {
        this.proxyService = proxyService;
        this.hostKey = hostKey;
    }

    public Object execute(final RemoteCommand command, RemoteBehavior behavior) {
        try {
            command.setHostKey(hostKey);
            ServiceClientResponse execute = command.execute(proxyService, behavior);
            return command.handleResponse(execute);
        } catch (ProxyRequestException ex) {
            throw new RemoteConnectionException("Error communicating with remote node", ex);
        } catch (IOException ex) {
            throw new DatastoreOperationException("Error handling response", ex);
        }
    }

}
