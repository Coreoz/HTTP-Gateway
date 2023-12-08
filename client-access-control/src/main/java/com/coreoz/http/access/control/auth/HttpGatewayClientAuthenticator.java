package com.coreoz.http.access.control.auth;

import play.mvc.Http;

import java.util.List;

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

    static class HttpGatewayMergedClientAuthenticator implements HttpGatewayClientAuthenticator {
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
