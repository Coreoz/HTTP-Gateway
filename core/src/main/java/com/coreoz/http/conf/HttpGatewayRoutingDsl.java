package com.coreoz.http.conf;

import play.routing.Router;
import play.routing.RoutingDsl;

/**
 * A simple DSL to add routing in HTTP Gateway.<br>
 * Contrary to the base Play {@link RoutingDsl} class, this DSL provide the feature to configure multi routes at once.
 * See {@link #addRoutes(HttpGatewayRouterConfiguration)}.
 */
public class HttpGatewayRoutingDsl {
    private final RoutingDsl routingDsl;

    public HttpGatewayRoutingDsl(RoutingDsl routingDsl) {
        this.routingDsl = routingDsl;
    }

    /**
     * Add routes to the router
     * @param routerConfiguration The function that will add routes to the Play {@link RoutingDsl}.
     * @return The current {@link HttpGatewayRoutingDsl} instance
     */
    public HttpGatewayRoutingDsl addRoutes(HttpGatewayRouterConfiguration routerConfiguration) {
        routerConfiguration.configureRoutes(routingDsl);
        return this;
    }

    /**
     * Build the Play router: should be called only internally
     */
    public Router build() {
        return routingDsl.build();
    }
}
