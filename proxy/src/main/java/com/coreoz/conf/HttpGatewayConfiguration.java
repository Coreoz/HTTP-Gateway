package com.coreoz.conf;

import lombok.Value;

@Value
public class HttpGatewayConfiguration {
    int httpPort;
    HttpGatewayRouterConfiguration routerConfiguration;
}
