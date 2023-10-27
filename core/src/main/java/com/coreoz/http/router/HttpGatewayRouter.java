package com.coreoz.http.router;

import com.coreoz.http.router.data.*;

import java.util.Map;
import java.util.Optional;

// TODO to comment and to put in a dedicated module
public class HttpGatewayRouter {
    private final Map<String, IndexedEndpoints> routerIndex;

    public HttpGatewayRouter(Iterable<HttpEndpoint> endpoints) {
        this.routerIndex = SearchRouteIndexer.indexEndpoints(endpoints);
    }

    /**
     * @param routerIndex The indexed endpoint by HTTP method: <code>{GET: IndexedEndpoints, POST: IndexedEndpoints, ...}</code>
     */
    public HttpGatewayRouter(Map<String, IndexedEndpoints> routerIndex) {
        this.routerIndex = routerIndex;
    }

    /**
     * Add a new endpoint to the indexed routes.
     * @return The endpoint passed as an argument if it the new route was added. If there were an already existing route
     * for the specified path, then the new endpoint is NOT added and the existing endpoint is returned.
     */
    public HttpEndpoint addEndpoint(HttpEndpoint endpoint) {
        EndpointParsedData addedEndpoint = SearchRouteIndexer.addEndpointToIndex(routerIndex, endpoint);
        return addedEndpoint == null ? null : addedEndpoint.getHttpEndpoint();
    }

    // SEARCH

    public Optional<MatchingRoute> searchRoute(String method, String requestPath) {
        IndexedEndpoints methodIndex = routerIndex.get(method);
        if (methodIndex == null) {
            return Optional.empty();
        }
        return SearchRouteEngine.searchRoute(methodIndex, requestPath);
    }

    public DestinationRoute computeDestinationRoute(MatchingRoute matchingRoute, String destinationBaseUrl) {
        return SearchRouteEngine.computeDestinationRoute(matchingRoute, destinationBaseUrl);
    }
}
