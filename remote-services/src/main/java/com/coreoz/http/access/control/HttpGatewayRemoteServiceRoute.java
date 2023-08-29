package com.coreoz.http.access.control;

import lombok.Value;

@Value
public class HttpGatewayRemoteServiceRoute {
    String routeId;
    String method;
    String path;
}
