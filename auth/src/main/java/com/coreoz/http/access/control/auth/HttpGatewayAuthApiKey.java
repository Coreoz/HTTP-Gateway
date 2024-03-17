package com.coreoz.http.access.control.auth;

public record HttpGatewayAuthApiKey(String authKey) {
    public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";
}
