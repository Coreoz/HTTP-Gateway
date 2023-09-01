package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayAuthApiKey {
    public static final String AUTHORIZATION_BEARER = "Bearer ";

    String objectId;
    String authKey;
}
