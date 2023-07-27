package com.coreoz.http.play;

import com.coreoz.http.conf.HttpGatewayRequestHandler;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import play.mvc.Http;
import play.mvc.Result;
import play.routing.Router;
import play.routing.RoutingDsl;

import java.util.concurrent.CompletionStage;

public class HttpGatewayAsyncRoutesConfigurator implements HttpGatewayRouterConfiguration {
    private final HttpGatewayRequestHandler requestHandler;

    public HttpGatewayAsyncRoutesConfigurator(HttpGatewayRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public Router configureRoutes(RoutingDsl routingDsl) {
        return routingDsl
            .GET("/*to")
            .routingAsync(this::handleRequest)
            .POST("/*to")
            .routingAsync(this::handleRequest)
            .PUT("/*to")
            .routingAsync(this::handleRequest)
            .PATCH("/*to")
            .routingAsync(this::handleRequest)
            .DELETE("/*to")
            .routingAsync(this::handleRequest)
            .build();
    }

    private CompletionStage<Result> handleRequest(Http.Request request, Object to) {
        return requestHandler.handleRequest(request);
    }
}
