package com.coreoz.http.conf;

/**
 * The routing function that adds routes to {@link HttpGatewayRoutingDsl}.
 */
@FunctionalInterface
public interface HttpGatewayRoutingDslConfiguration {
    HttpGatewayRoutingDsl configure(HttpGatewayRoutingDsl routingDsl);
}
