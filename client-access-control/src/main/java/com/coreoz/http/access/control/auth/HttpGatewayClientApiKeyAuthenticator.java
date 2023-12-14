package com.coreoz.http.access.control.auth;

import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Http.Request;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HttpGatewayClientApiKeyAuthenticator implements HttpGatewayClientAuthenticator {
    private final Map<String, String> clientIndexedByApiKey;

    public HttpGatewayClientApiKeyAuthenticator(List<HttpGatewayAuthApiKey> clients) {
        this.clientIndexedByApiKey = clients.stream().collect(Collectors.toMap(
            HttpGatewayAuthApiKey::getAuthKey,
            HttpGatewayAuthApiKey::getObjectId
        ));
    }

    // TODO faire un TU avec un second authenticator

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
        if(authorizationHeaderValue != null
            && authorizationHeaderValue.startsWith(HttpGatewayAuthApiKey.AUTHORIZATION_BEARER_PREFIX)
        ) {
            return authorizationHeaderValue.substring(HttpGatewayAuthApiKey.AUTHORIZATION_BEARER_PREFIX.length());
        }

        return null;
    }
}
