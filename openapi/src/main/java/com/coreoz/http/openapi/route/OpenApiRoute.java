package com.coreoz.http.openapi.route;

import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.openapi.service.OpenApiFetchingService;
import com.coreoz.http.play.responses.JsonContent;
import com.coreoz.http.validation.HttpGatewayClientValidator;
import io.swagger.v3.oas.models.OpenAPI;
import play.mvc.Results;
import play.routing.RoutingDsl;

public class OpenApiRoute implements HttpGatewayRouterConfiguration {

    private final HttpGatewayClientValidator clientValidator;
    private OpenAPI openApiDefinition;

    // TODO HttpGatewayRemoteServicesIndex
    // TODO OpenAPI configuration(remote openapi auth + URL), with default
    public OpenApiRoute(HttpGatewayClientValidator clientValidator, OpenApiFetchingService fetchingService) {
        this.clientValidator = clientValidator;
        fetchingService.fetch().thenAccept(openApiDefinition -> {
            OpenApiRoute.this.openApiDefinition = openApiDefinition;
        });
    }

    @Override
    public void configureRoutes(RoutingDsl routingDsl) {
        routingDsl.GET("/openapi").routingTo(request -> {
            // TODO optional authentication
            // TODO show only schema definition with routes visible for the authenticated client
            if (openApiDefinition != null) {
                return Results.ok(new JsonContent(openApiDefinition));
            }
            return Results.ok("Open API definitions not available");
        });
    }
}
