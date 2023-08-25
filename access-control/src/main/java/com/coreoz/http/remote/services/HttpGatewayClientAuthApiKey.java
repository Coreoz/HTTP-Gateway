package com.coreoz.http.remote.services;

import lombok.Value;

@Value
public class HttpGatewayClientAuthApiKey {
    String clientId;
    String authKey;
}
