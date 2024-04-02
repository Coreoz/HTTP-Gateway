package com.coreoz.http.openapi.route;

import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Results;
import play.routing.RoutingDsl;

/**
 * The OpenAPI route that can be added to an HTTP Gateway project.<br>
 * Although this route could in theory be added to any Play project.
 */
@Slf4j
public class OpenApiRoute implements HttpGatewayRouterConfiguration {
    private final String routePath;
    private OpenAPI openApiDefinition;

    public OpenApiRoute(OpenApiRouteConfiguration routeConfiguration) {
        this.routePath = routeConfiguration.routePath();
        OpenApiOrchestrator
            .computeUnifiedOpenApi(routeConfiguration)
            .thenAccept(openApiDefinition -> {
                OpenApiRoute.this.openApiDefinition = openApiDefinition;
                logger.info("Open API definitions loaded");
            }).exceptionally(error -> {
                logger.error("Failed to load API definition", error);
                return null;
            });
    }

    /**
     * Add the OpenAPI route to the Play routing DSL
     * @param routingDsl Play router
     */
    @Override
    public void configureRoutes(RoutingDsl routingDsl) {
        routingDsl.GET(routePath).routingTo(request -> {
            // TODO optional client authentication
            // TODO show only schema definition with routes visible for the authenticated client
            if (openApiDefinition != null) {
                try {
                    return Results.ok(Yaml.mapper().writeValueAsString(openApiDefinition));
                } catch (JsonProcessingException e) {
                    return Results.internalServerError(e.getMessage());
                }
            }
            return Results.ok("Open API definitions not available");
        });
    }
}
