package com.coreoz.conf;

import play.routing.Router;
import play.routing.RoutingDsl;

@FunctionalInterface
public interface HttpGatewayRouterConfiguration {
    Router configureRoutes(RoutingDsl routingDsl);
}
