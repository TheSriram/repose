package com.rackspace.papi.service.proxy.httpcomponent;

import com.rackspace.papi.http.proxy.common.AbstractRequestProcessor;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;

/**
 * Process a request to copy over header values, query string parameters, and
 * request body as necessary.
 * 
 */
class HttpComponentRequestProcessor extends AbstractRequestProcessor {
    private final HttpServletRequest sourceRequest;

    public HttpComponentRequestProcessor(HttpServletRequest request) {
      this.sourceRequest = request;
    }

    /**
     * Copy request parameters (query string) from source request to the
     * http method.
     * 
     * @param method 
     */
    private void setRequestParameters(HttpRequestBase method) {
      Enumeration<String> names = sourceRequest.getParameterNames();

      while (names.hasMoreElements()) {
        String name = names.nextElement();
        String[] values = sourceRequest.getParameterValues(name);
        for (String value: values) {
          method.getParams().setParameter(name, value);
        }
      }
    }
    
    /**
     * Copy header values from source request to the http method.
     * 
     * @param method 
     */
    private void setHeaders(HttpRequestBase method) {
        final Enumeration<String> headerNames = sourceRequest.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            final String headerName = headerNames.nextElement();
            
            if (excludeHeader(headerName)) {
               continue;
            }

            final Enumeration<String> headerValues = sourceRequest.getHeaders(headerName);

            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                method.addHeader(headerName, headerValue);
            }
        }
    }

    /**
     * Process a base http request.  Base http methods will not contain a
     * message body.
     * 
     * @param method
     * @return 
     */
    public HttpRequestBase process(HttpRequestBase method) {
      setHeaders(method);
      setRequestParameters(method);
      return method;

    }

    /**
     * Process an entity enclosing http method.  These methods can handle
     * a request body.
     * 
     * @param method
     * @return
     * @throws IOException 
     */
    public HttpRequestBase process(HttpEntityEnclosingRequestBase method) throws IOException {
      setHeaders(method);
      setRequestParameters(method);
      method.setEntity(new InputStreamEntity(sourceRequest.getInputStream(), sourceRequest.getContentLength()));
      return method;
    }
}