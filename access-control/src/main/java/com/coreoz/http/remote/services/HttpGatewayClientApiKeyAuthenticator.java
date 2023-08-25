package com.coreoz.http.remote.services;

import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Http.Request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TODO add an authenticator interface
@Slf4j
public class HttpGatewayClientApiKeyAuthenticator {
    private static final String AUTHORIZATION_BEARER = "Bearer ";

    private final Map<String, String> clientIndexedByApiKey;

    public HttpGatewayClientApiKeyAuthenticator(List<HttpGatewayClientAuthApiKey> clients) {
        this.clientIndexedByApiKey = clients.stream().collect(Collectors.toMap(
            HttpGatewayClientAuthApiKey::getAuthKey,
            HttpGatewayClientAuthApiKey::getClientId
        ));
    }

    public String authenticate(Request downstreamRequest) {
        String authorizationHeaderValue = downstreamRequest.header(HttpHeaders.AUTHORIZATION).orElse(null);
        String apiKey = extractApiKeyFromRequest(authorizationHeaderValue);
        if (apiKey == null) {
            logger.warn(
                "Authentication failed: missing authentication API Key on IP {}, Authorization header value is {}",
                downstreamRequest.remoteAddress(),
                authorizationHeaderValue
            );
            return null;
        }
        String clientId = clientIndexedByApiKey.get(apiKey);
        if (clientId == null) {
            logger.warn(
                "Authentication failed: not client is recognized for API Key '{}' on IP {}, Authorization header value is {}",
                apiKey,
                downstreamRequest.remoteAddress(),
                authorizationHeaderValue
            );
            return null;
        }
        return clientId;
    }

    private static String extractApiKeyFromRequest(String authorizationHeaderValue) {
        if(authorizationHeaderValue != null) {
            if(authorizationHeaderValue.startsWith(AUTHORIZATION_BEARER)) {
                return authorizationHeaderValue.substring(AUTHORIZATION_BEARER.length());
            }
        }

        return null;
    }
}
