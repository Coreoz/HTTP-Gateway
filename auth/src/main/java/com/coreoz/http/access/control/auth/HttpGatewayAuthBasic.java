package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayAuthBasic {
    public static final String AUTHORIZATION_BASIC_PREFIX = "Basic ";

    // The object id referenced by this authentication, it can be a clientId or a serviceId
    String objectId;
    String userId;
    String password;
}
