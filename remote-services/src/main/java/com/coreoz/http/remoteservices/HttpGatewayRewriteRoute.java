package com.coreoz.http.remoteservices;

import lombok.Value;

@Value
public class HttpGatewayRewriteRoute {
    String gatewayPath;
    String routeId;
}
