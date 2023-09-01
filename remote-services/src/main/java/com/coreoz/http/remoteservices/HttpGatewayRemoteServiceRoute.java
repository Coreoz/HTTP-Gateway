package com.coreoz.http.remoteservices;

import lombok.Value;

@Value
public class HttpGatewayRemoteServiceRoute {
    String routeId;
    String method;
    String path;
}
