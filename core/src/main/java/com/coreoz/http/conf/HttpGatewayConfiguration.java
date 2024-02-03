package com.coreoz.http.conf;

import lombok.Value;

@Value
public class HttpGatewayConfiguration {
    int httpPort;
    HttpGatewayRoutingDslConfiguration routingDsl;
}
