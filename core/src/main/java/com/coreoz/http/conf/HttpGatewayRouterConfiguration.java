package com.coreoz.http.conf;

import com.coreoz.http.play.HttpGatewayAsyncRoutesConfigurator;
import play.routing.RoutingDsl;

/**
 * HTTP Gateway router configuration.<br>
 * <br>
 * Main routing should be creating using {@link #asyncRouting(HttpGatewayRequestHandler)}.<br>
 * Additional routing can be configured using the {@link #configureRoutes(RoutingDsl)}.
 */
@FunctionalInterface
public interface HttpGatewayRouterConfiguration {
    /**
     * Add route to Play router
     * @param routingDsl Play router
     */
    void configureRoutes(RoutingDsl routingDsl);

    /**
     * Map GET, POST, PUT, PATCH, DELETE routes to the {@link HttpGatewayRequestHandler} function passed
     * as parameter. See {@link HttpGatewayRouterConfiguration} for details.<br>
     * This is used to create the main HTTP Gateway handler.
     * @param requestHandler The function to handle
     * @return The HTTP Gateway router configuration function
     */
    static HttpGatewayRouterConfiguration asyncRouting(HttpGatewayRequestHandler requestHandler) {
        return new HttpGatewayAsyncRoutesConfigurator(requestHandler);
    }
}
