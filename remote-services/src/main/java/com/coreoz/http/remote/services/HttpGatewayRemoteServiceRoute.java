package com.coreoz.http.remote.services;

import lombok.Value;

@Value
public class HttpGatewayRemoteServiceRoute {
    String routeId;
    String method;
    String path;
}
