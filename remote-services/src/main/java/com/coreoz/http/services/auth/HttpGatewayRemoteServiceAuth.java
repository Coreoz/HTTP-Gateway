package com.coreoz.http.services.auth;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import lombok.Value;

@Value
public class HttpGatewayRemoteServiceAuth {
    String serviceId;
    HttpGatewayUpstreamAuthenticator authenticator;
}
