package com.coreoz.http.access.control;

import lombok.Value;

@Value
public class HttpGatewayRewriteRoute {
    String gatewayPath;
    String routeId;
}
