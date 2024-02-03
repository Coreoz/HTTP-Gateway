package com.coreoz.http.play;

import com.coreoz.http.conf.HttpGatewayRequestHandler;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.RoutingDsl;

import java.util.concurrent.CompletionStage;

/**
 * Configure all routes to point to a single route handler: {@link HttpGatewayRequestHandler}.<br>
 * <br>
 * To add custom routing, other routes must be added in the {@link RoutingDsl} before this route.
 */
public class HttpGatewayAsyncRoutesConfigurator implements HttpGatewayRouterConfiguration {
    private final HttpGatewayRequestHandler requestHandler;

    public HttpGatewayAsyncRoutesConfigurator(HttpGatewayRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public void configureRoutes(RoutingDsl routingDsl) {
        routingDsl
            .GET("/*to")
            .routingAsync(this::handleRequest)
            .POST("/*to")
            .routingAsync(this::handleRequest)
            .PUT("/*to")
            .routingAsync(this::handleRequest)
            .PATCH("/*to")
            .routingAsync(this::handleRequest)
            .DELETE("/*to")
            .routingAsync(this::handleRequest);
    }

    private CompletionStage<Result> handleRequest(Http.Request request, Object to) {
        return requestHandler.handleRequest(request);
    }
}
