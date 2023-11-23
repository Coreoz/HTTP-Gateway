package com.coreoz.http.validation;

import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.mvc.Http;

/**
 * Static version of {@link HttpGatewayClientValidator}
 */
public class HttpGatewayClientValidators {
    /**
     * Identify a client.
     * @return The validated clientId, else an {@link HttpResponseStatus#UNAUTHORIZED} error
     */
    public static HttpGatewayValidation<String> validateClientIdentification(
        HttpGatewayClientAuthenticator gatewayClientAuthenticator, Http.Request downstreamRequest
    ) {
        String clientId = gatewayClientAuthenticator.authenticate(downstreamRequest);
        if (clientId == null) {
            return HttpGatewayValidation.ofError(HttpResponseStatus.UNAUTHORIZED, "Client authentication failed");
        }
        return HttpGatewayValidation.ofValue(clientId);
    }
}
