package com.coreoz.http.router;

import com.coreoz.http.router.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Handle routing resolving for a downstream path to an upstream route.<br>
 * This should be used:<br>
 * 1. To index the available routes (using constructor, and if necessary using @{link {@link #addEndpoint(HttpEndpoint)}})<br>
 * 2. To search for a route for a method and a path using {@link #searchRoute(String, String)}<br>
 * 3. To compute the full upstream destination URL using {@link #computeDestinationRoute(MatchingRoute, String)}
 */
public class HttpGatewayRouter {
    private final @NotNull Map<String, IndexedEndpoints> routerIndex;

    public HttpGatewayRouter(@NotNull Iterable<HttpEndpoint> endpoints) {
        this.routerIndex = SearchRouteIndexer.indexEndpoints(endpoints);
    }

    /**
     * @param routerIndex The indexed endpoint by HTTP method: <code>{GET: IndexedEndpoints, POST: IndexedEndpoints, ...}</code>
     */
    public HttpGatewayRouter(@NotNull Map<String, IndexedEndpoints> routerIndex) {
        this.routerIndex = routerIndex;
    }

    /**
     * Add a new endpoint to the indexed routes.
     * @return The endpoint passed as an argument if it is the new route was added. If there were an already existing route
     * for the specified path, then the new endpoint is NOT added and the existing endpoint is returned.
     */
    public @NotNull HttpEndpoint addEndpoint(@NotNull HttpEndpoint endpoint) {
        EndpointParsedData addedEndpoint = SearchRouteIndexer.addEndpointToIndex(routerIndex, endpoint);
        return addedEndpoint.getHttpEndpoint();
    }

    // SEARCH

    /**
     * Search a route in the index
     * @param method The HTTP method, like GET or POST
     * @param downstreamPath The searched path, like /users
     * @return The optional matching route
     */
    public @NotNull Optional<MatchingRoute> searchRoute(@NotNull String method, @NotNull String downstreamPath) {
        IndexedEndpoints methodIndex = routerIndex.get(method);
        if (methodIndex == null) {
            return Optional.empty();
        }
        return SearchRouteEngine.searchRoute(methodIndex, downstreamPath);
    }

    /**
     * Compute the full upstream destination URL, like <code>http://remote-service.com/users/123/payments</code>
     * @param matchingRoute A matching found using @{link {@link #searchRoute(String, String)}}
     * @param destinationBaseUrl The base URL of the upstream destination, like <code>http://remote-service.com</code> (without a slash at the end)
     * @return The full computed URL
     */
    public @NotNull DestinationRoute computeDestinationRoute(@NotNull MatchingRoute matchingRoute, @Nullable String destinationBaseUrl) {
        return SearchRouteEngine.computeDestinationRoute(matchingRoute, destinationBaseUrl);
    }
}
