package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayAuthBasic implements HttpGatewayAuthObject {
    public static final String AUTHORIZATION_BASIC_PREFIX = "Basic ";

    String objectId;
    String userId;
    String password;
}
