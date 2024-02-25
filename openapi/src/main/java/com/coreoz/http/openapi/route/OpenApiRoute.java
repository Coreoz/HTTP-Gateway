package com.coreoz.http.openapi.route;

import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.openapi.service.OpenApiFetchingService;
import com.coreoz.http.validation.HttpGatewayClientValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Results;
import play.routing.RoutingDsl;

@Slf4j
public class OpenApiRoute implements HttpGatewayRouterConfiguration {

    private final HttpGatewayClientValidator clientValidator;
    private OpenAPI openApiDefinition;

    // TODO OpenAPI configuration(remote openapi auth + URL), base OpenAPI config, with default => builder configuration, optional authentication & OpenAPI cleaning
    public OpenApiRoute(HttpGatewayClientValidator clientValidator, OpenApiFetchingService fetchingService) {
        this.clientValidator = clientValidator;
        fetchingService.fetch().thenAccept(openApiDefinition -> {
            OpenApiRoute.this.openApiDefinition = openApiDefinition;
            logger.info("Open API definitions loaded");
        }).exceptionally(error -> {
            logger.error("Failed to load API definition", error);
            return null;
        });
    }

    @Override
    public void configureRoutes(RoutingDsl routingDsl) {
        // TODO give the ability to customize the path
        routingDsl.GET("/openapi").routingTo(request -> {
            // TODO optional authentication
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
