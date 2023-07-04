package com.coreoz.router;

import com.coreoz.router.beans.EndpointParsedData;
import com.coreoz.router.beans.HttpEndpoint;
import com.coreoz.router.beans.IndexedEndpoints;
import com.coreoz.router.beans.MatchingRoute;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpGatewayRouter<T> {
    private final Map<String, IndexedEndpoints<T>> routerIndex;

    public HttpGatewayRouter(List<HttpEndpoint<T>> endpoints) {
        this.routerIndex = SearchRouteIndexer.indexEndpoints(endpoints);
    }

    /**
     * Add a new endpoint to the indexed routes.
     * @return The endpoint passed as an argument if it the new route was added. If there were an already existing route
     * for the specified path, then the new endpoint is NOT added and the existing endpoint is returned.
     */
    public HttpEndpoint<T> addEndpoint(HttpEndpoint<T> endpoint) {
        EndpointParsedData<T> addedEndpoint = SearchRouteIndexer.addEndpointToIndex(endpoint, routerIndex);
        return addedEndpoint == null ? null : addedEndpoint.getHttpEndpoint();
    }

    // SEARCH

    public Optional<MatchingRoute<T>> searchRoute(String method, String requestPath) {
        IndexedEndpoints<T> methodIndex = routerIndex.get(method);
        if (methodIndex == null) {
            return Optional.empty();
        }
        return SearchRouteEngine.searchRoute(methodIndex, requestPath);
    }
}
