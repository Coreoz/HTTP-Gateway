package com.coreoz.http.access.control.auth;

public record HttpGatewayAuthBasic(String userId, String password) {
    public static final String AUTHORIZATION_BASIC_PREFIX = "Basic ";
}
