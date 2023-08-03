package com.coreoz.http.remote.services;

import lombok.Value;

@Value
public class HttpGatewayRewriteRoute {
    String gatewayPath;
    String routeId;
}
