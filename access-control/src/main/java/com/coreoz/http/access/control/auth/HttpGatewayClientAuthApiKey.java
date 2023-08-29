package com.coreoz.http.access.control.auth;

import lombok.Value;

@Value
public class HttpGatewayClientAuthApiKey {
    String clientId;
    String authKey;
}
