package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayAuthApiKey {
    public static final String AUTHORIZATION_BEARER_PREFIX = "Bearer ";

    // The object id referenced by this authentication, it can be a clientId or a serviceId
    String objectId;
    String authKey;
}
