package com.coreoz.conf;

import com.coreoz.router.HttpGatewayAsyncRouter;
import play.routing.Router;
import play.routing.RoutingDsl;

@FunctionalInterface
public interface HttpGatewayRouterConfiguration {
    Router configureRoutes(RoutingDsl routingDsl);

    static HttpGatewayRouterConfiguration asyncRouter(HttpGatewayRequestHandler requestHandler) {
        return new HttpGatewayAsyncRouter(requestHandler);
    }
}
