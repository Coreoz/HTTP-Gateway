package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayAuthApiKey implements HttpGatewayAuthObject {
    public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

    String objectId;
    String authKey;
}
