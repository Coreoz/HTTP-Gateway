package com.coreoz.http.access.control.auth;

import play.mvc.Http;

import java.util.List;

public interface HttpGatewayAuthenticator {
    /**
     * Authenticate a client from an incoming HTTP Gateway request
     * @param downstreamRequest The incoming HTTP request
     * @return The clientId if the authentication is successful, else null
     */
    String authenticate(Http.Request downstreamRequest);

    static HttpGatewayAuthenticator merge(List<HttpGatewayAuthenticator> authenticators) {
        return new HttpGatewayMergedAuthenticator(authenticators);
    }

    static class HttpGatewayMergedAuthenticator implements HttpGatewayAuthenticator {
        private final List<HttpGatewayAuthenticator> authenticators;

        public HttpGatewayMergedAuthenticator(List<HttpGatewayAuthenticator> authenticators) {
            this.authenticators = authenticators;
        }

        @Override
        public String authenticate(Http.Request downstreamRequest) {
            for (HttpGatewayAuthenticator authenticator : authenticators) {
                String clientId = authenticator.authenticate(downstreamRequest);
                if (clientId != null) {
                    return clientId;
                }
            }
            return null;
        }
    }
}
