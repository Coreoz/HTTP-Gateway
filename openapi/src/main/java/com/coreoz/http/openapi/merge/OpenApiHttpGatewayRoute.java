package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.HttpGatewayRoute;
import com.coreoz.http.routes.parsing.HttpRouteDefinition;
import com.coreoz.http.routes.router.HttpRoute;
import org.jetbrains.annotations.NotNull;

/**
 * A route that wraps an {@link HttpGatewayRoute} in order to expose the upstream path instead of the downstream path
 * in the {@link HttpRouteDefinition}.
 */
public class OpenApiHttpGatewayRoute implements HttpRoute {
    private final HttpGatewayRoute route;
    private final String upstreamPath;

    public OpenApiHttpGatewayRoute(HttpGatewayRoute route, String upstreamPath) {
        this.route = route;
        this.upstreamPath = upstreamPath;
    }

    public HttpGatewayRoute getBaseRoute() {
        return route;
    }

    @Override
    public @NotNull String routeId() {
        return route.routeId();
    }

    @Override
    public @NotNull String method() {
        return route.method();
    }

    @Override
    public @NotNull String path() {
        return upstreamPath;
    }
}
