package com.coreoz.http.access.control.auth;

import com.google.common.net.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Http.Request;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An upstream client authenticator using Basic Authentication
 */
@Slf4j
public class HttpGatewayClientBasicAuthenticator implements HttpGatewayClientAuthenticator {
    private final Map<String, HttpGatewayAuthBasic> clientIndexedByBasicLogin;

    public HttpGatewayClientBasicAuthenticator(List<HttpGatewayAuthBasic> clients) {
        this.clientIndexedByBasicLogin = clients.stream().collect(Collectors.toMap(
            HttpGatewayAuthBasic::getUserId,
            Function.identity()
        ));
    }

    public String authenticate(Request downstreamRequest) {
        String authorizationHeaderValue = downstreamRequest.header(HttpHeaders.AUTHORIZATION).orElse(null);
        Credentials suppliedBasicAuth = parseBasicHeader(authorizationHeaderValue);
        if (suppliedBasicAuth == null) {
            logger.info(
                "Authentication failed: missing authentication basic auth on IP {}, Authorization header value is {}",
                downstreamRequest.remoteAddress(),
                authorizationHeaderValue
            );
            return null;
        }

        HttpGatewayAuthBasic clientAuth = clientIndexedByBasicLogin.get(suppliedBasicAuth.userId());
        if (clientAuth == null) {
            logger.warn(
                "Authentication failed: not client is recognized for basic userId '{}' on IP {}, Authorization header value received is {}",
                suppliedBasicAuth.userId(),
                downstreamRequest.remoteAddress(),
                authorizationHeaderValue
            );
            return null;
        }
        if (!clientAuth.getPassword().equals(suppliedBasicAuth.password())) {
            logger.warn(
                "Authentication failed: wrong password for Basic Auth for client '{}' on IP {}, Authorization header value received is {}",
                suppliedBasicAuth.userId(),
                downstreamRequest.remoteAddress(),
                authorizationHeaderValue
            );
            return null;
        }
        return clientAuth.getObjectId();
    }

    private static Credentials parseBasicHeader(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }

        if (!authorizationHeader.startsWith(HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX)) {
            return null;
        }

        String[] decodedCredentials = new String(
            Base64.getDecoder().decode(authorizationHeader.substring(HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX.length())),
            StandardCharsets.UTF_8
        ).split(":", 2);

        if (decodedCredentials.length != 2) {
            logger.warn(
                "Basic Authorization header must use the form: {}base64(username:password), received: {}",
                HttpGatewayAuthBasic.AUTHORIZATION_BASIC_PREFIX,
                authorizationHeader
            );
            return null;
        }

        return new Credentials(decodedCredentials[0], decodedCredentials[1]);
    }

    private record Credentials(String userId, String password){}
}
