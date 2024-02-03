package com.coreoz.http.openapi.route;

import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import play.mvc.Results;
import play.routing.RoutingDsl;

public class OpenApiRoute implements HttpGatewayRouterConfiguration {

    @Override
    public void configureRoutes(RoutingDsl routingDsl) {
        routingDsl.GET("/openapi").routingTo(request -> Results.ok("API DOC"));
    }
}
