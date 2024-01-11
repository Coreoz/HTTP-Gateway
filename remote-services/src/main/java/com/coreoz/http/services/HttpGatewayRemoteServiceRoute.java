package com.coreoz.http.services;

import lombok.Value;

@Value
public class HttpGatewayRemoteServiceRoute {
    String routeId;
    String method;
    String path;
}
