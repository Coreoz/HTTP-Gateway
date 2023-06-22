package com.coreoz.conf;

import com.coreoz.play.HttpGatewayAsyncRoutesConfigurator;
import play.routing.Router;
import play.routing.RoutingDsl;

@FunctionalInterface
public interface HttpGatewayRouterConfiguration {
    Router configureRoutes(RoutingDsl routingDsl);

    static HttpGatewayRouterConfiguration asyncRouting(HttpGatewayRequestHandler requestHandler) {
        return new HttpGatewayAsyncRoutesConfigurator(requestHandler);
    }
}
