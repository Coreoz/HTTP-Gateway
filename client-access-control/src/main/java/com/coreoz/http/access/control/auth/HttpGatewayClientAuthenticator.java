package com.coreoz.http.access.control.auth;

import play.mvc.Http;

import java.util.List;

/**
 * Handle the authentication for the all client using the same authentication method.
 * For example, one single instance of {@code HttpGatewayClientApiKeyAuthenticator} will handle authentication
 * for all clients using the API key authentication method. It is why each authenticator contains an index of clients.
 */
public interface HttpGatewayClientAuthenticator {
    /**
     * Authenticate a client from an incoming HTTP Gateway request
     * @param downstreamRequest The incoming HTTP request
     * @return The clientId if the authentication is successful, else null
     */
    String authenticate(Http.Request downstreamRequest);

    static HttpGatewayClientAuthenticator merge(List<HttpGatewayClientAuthenticator> authenticators) {
        return new HttpGatewayMergedClientAuthenticator(authenticators);
    }

    class HttpGatewayMergedClientAuthenticator implements HttpGatewayClientAuthenticator {
        private final List<HttpGatewayClientAuthenticator> authenticators;

        public HttpGatewayMergedClientAuthenticator(List<HttpGatewayClientAuthenticator> authenticators) {
            this.authenticators = authenticators;
        }

        @Override
        public String authenticate(Http.Request downstreamRequest) {
            for (HttpGatewayClientAuthenticator authenticator : authenticators) {
                String clientId = authenticator.authenticate(downstreamRequest);
                if (clientId != null) {
                    return clientId;
                }
            }
            return null;
        }
    }
}
