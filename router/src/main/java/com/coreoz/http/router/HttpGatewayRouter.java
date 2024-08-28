package com.coreoz.http.router;

import com.coreoz.http.routes.HttpRouter;
import com.coreoz.http.routes.HttpRoutes;
import com.coreoz.http.routes.parsing.DestinationRoute;
import com.coreoz.http.routes.router.index.IndexedRoutes;
import com.coreoz.http.routes.router.search.RawMatchingRoute;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Handle routing resolving for a downstream path to an upstream route.<br>
 * This should be used:<br>
 * 1. To index the available routes (using constructor, and if necessary using @{link {@link #addRoute(HttpGatewayRoute)}})<br>
 * 2. To search for a route for a method and a path using {@link #searchRoute(String, String)}<br>
 * 3. To compute the full upstream destination URL using {@link #computeDestinationRoute(RawMatchingRoute)}
 */
public class HttpGatewayRouter {
    private final @NotNull HttpRouter<HttpGatewayRoute> httpRouter;

    public HttpGatewayRouter(@NotNull Iterable<HttpGatewayRoute> routes) {
        this.httpRouter = new HttpRouter<>(routes);
    }

    /**
     * @param routesIndex The indexed routes by HTTP method: <code>{GET: IndexedRoutes, POST: IndexedRoutes, ...}</code>
     */
    public HttpGatewayRouter(@NotNull Map<String, IndexedRoutes<HttpGatewayRoute>> routesIndex) {
        this.httpRouter = new HttpRouter<>(routesIndex);
    }

    /**
     * Add a new route to the indexed routes.
     * @return The route passed as an argument if it was correctly added to the index. If there was already an existing route
     * for the specified path, then the new route is NOT added and the existing route in the index is returned.
     */
    public @NotNull HttpGatewayRoute addRoute(@NotNull HttpGatewayRoute route) {
        return this.httpRouter.addRoute(route);
    }

    // SEARCH

    /**
     * Search a route in the index
     *
     * @param method         The HTTP method, like GET or POST
     * @param downstreamPath The searched path, like /users
     * @return The optional matching route
     */
    public @NotNull Optional<RawMatchingRoute<HttpGatewayRoute>> searchRoute(@NotNull String method, @NotNull String downstreamPath) {
        return this.httpRouter.searchRoute(method, downstreamPath);
    }

    /**
     * Compute the upstream destination path, like <code>/users/123/payments</code>
     * @param matchingRoute A matching found using @{link {@link #searchRoute(String, String)}}
     * @return The computed destination path
     */
    public @NotNull DestinationRoute computeDestinationRoute(@NotNull RawMatchingRoute<HttpGatewayRoute> matchingRoute) {
        return HttpRoutes.computeDestinationRoute(matchingRoute, matchingRoute.matchingRouteLeaf().httpRoute().upstreamParsedPath());
    }
}
