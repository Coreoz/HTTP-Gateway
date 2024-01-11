package com.coreoz.http.services;

import lombok.Value;

@Value
public class HttpGatewayRewriteRoute {
    String routeId;
    String downstreamPath;
}
