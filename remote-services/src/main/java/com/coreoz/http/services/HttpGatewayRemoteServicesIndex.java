package com.coreoz.http.services;

import com.coreoz.http.router.SearchRouteIndexer;
import com.coreoz.http.router.data.*;
import com.coreoz.http.exception.HttpGatewayValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Index remote services routes, so it can be used in a HttpGatewayRouter
 */
public class HttpGatewayRemoteServicesIndex {
    private final Map<String, HttpGatewayRemoteService> servicesByRouteId;
    private final List<HttpGatewayRemoteService> services;
    private final Map<String, String> gatewayRewriteRoutes;

    /**
     * Create the route index
     * @param services The available services
     * @param rewriteRoutes The routes for which the HTTP Gateway path is different from the service route
     * @throws HttpGatewayValidationException If the same routeId is used in multiple routes
     */
    public HttpGatewayRemoteServicesIndex(List<HttpGatewayRemoteService> services, List<HttpGatewayRewriteRoute> rewriteRoutes) {
        this.services = services;
        this.servicesByRouteId = services
            .stream()
            .flatMap(service -> service.getRoutes().stream().map(route -> Map.entry(route.getRouteId(), service)))
            .collect(Collector.of(
                HashMap::new,
                (map, routeEntry) -> {
                    if (map.put(routeEntry.getKey(), routeEntry.getValue()) != null) {
                        throw new HttpGatewayValidationException("Duplicate route-id for route '"+routeEntry.getKey()+"'");
                    }
                },
                (mapA, mapB) -> { throw new UnsupportedOperationException("Should not run concurrently"); },
                Collector.Characteristics.IDENTITY_FINISH
            ));
        this.gatewayRewriteRoutes = rewriteRoutes
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRewriteRoute::getRouteId,
                HttpGatewayRewriteRoute::getDownstreamPath
            ));
    }

    /**
     * Returns the available services
     */
    public List<HttpGatewayRemoteService> getServices() {
        return services;
    }

    private Stream<HttpEndpoint> routesStream() {
        return services
            .stream()
            .flatMap(service -> service.getRoutes().stream().map(route -> new HttpEndpoint(
                route.getRouteId(),
                route.getMethod(),
                gatewayRewriteRoutes.getOrDefault(route.getRouteId(), route.getPath()),
                route.getPath()
            )));
    }

    /**
     * Computes the route as a <code>Iterable<HttpEndpoint></code> object that can be used
     * in the router <code>HttpGatewayRouter</code> class
     */
    public Iterable<HttpEndpoint> computeRoutes() {
        return routesStream()::iterator;
    }

    /**
     * Does the same as {@link #computeRoutes()} but validate the routes consistency at the same time.
     * @throws HttpGatewayValidationException If there is an inconsistency detected
     */
    public Map<String, IndexedEndpoints> computeValidatedIndexedRoutes() {
        validateRewriteRoutes();

        Map<String, IndexedEndpoints> indexedEndpoints = new HashMap<>();
        routesStream().forEach(endpoint -> {
            if (endpoint.getDownstreamPath() == null) {
                throw new HttpGatewayValidationException(
                    "Downstream path cannot be null for route '" + endpoint.getRouteId() + "'"
                );
            }
            if (!endpoint.getDownstreamPath().startsWith("/")) {
                throw new HttpGatewayValidationException(
                    "Downstream path '" + endpoint.getDownstreamPath() + "' must start with a / for route '" + endpoint.getRouteId() + "'"
                );
            }

            if (endpoint.getUpstreamPath() == null) {
                throw new HttpGatewayValidationException("Upstream path must not be null for route '" + endpoint.getRouteId());
            }

            EndpointParsedData addedEndpoint = SearchRouteIndexer.addEndpointToIndex(indexedEndpoints, endpoint);
            if (addedEndpoint == null) {
                throw new HttpGatewayValidationException("Error reading endpoint " + endpoint);
            }
            if (addedEndpoint.getHttpEndpoint() != endpoint) {
                throw new HttpGatewayValidationException(
                    "Duplicate downstream path for routes '"
                    + addedEndpoint.getHttpEndpoint().getRouteId()
                    + "' and 'route-b': the endpoint "
                    + endpoint + " overlaps the path of the endpoint " + addedEndpoint.getHttpEndpoint()
                );
            }
        });
        return indexedEndpoints;
    }

    private void validateRewriteRoutes() {
        Map<String, HttpGatewayRemoteServiceRoute> indexedRoutesById = services
            .stream()
            .flatMap(service -> service.getRoutes().stream())
            .collect(Collectors.toMap(
                HttpGatewayRemoteServiceRoute::getRouteId,
                Function.identity()
            ));
        for (Map.Entry<String, String> rewriteRouteEntry : gatewayRewriteRoutes.entrySet()) {
            HttpGatewayRemoteServiceRoute initialRoute = indexedRoutesById.get(rewriteRouteEntry.getKey());
            if (initialRoute == null) {
                throw new HttpGatewayValidationException("Incorrect rewrite route that references a non existing routeId: " + rewriteRouteEntry.getKey());
            }

            String rewriteRoutePath = gatewayRewriteRoutes.get(rewriteRouteEntry.getKey());
            if (!areRouteCompatible(initialRoute.getPath(), rewriteRoutePath)) {
                throw new HttpGatewayValidationException("Incorrect rewrite route for routeId='" + rewriteRouteEntry.getKey() +
                    "', rewrite route path does not match initial route path, the paths pattern (e.g. {pattern}) names must be identical. Received: "
                    + initialRoute.getPath() + " (initial) != " + rewriteRoutePath + " (rewrite)");
            }
        }
    }

    private boolean areRouteCompatible(String initialPath, String rewritePath) {
        return extractPatternNames(SearchRouteIndexer.parseEndpoint(initialPath))
            .equals(extractPatternNames(SearchRouteIndexer.parseEndpoint(rewritePath)));
    }

    private Set<String> extractPatternNames(List<ParsedSegment> routeSegments) {
        return routeSegments
            .stream()
            .filter(ParsedSegment::isPattern)
            .map(ParsedSegment::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Verify if a route exists
     * @return True if there is a route that matches the routeId
     */
    public boolean hasRoute(String routeId) {
        return servicesByRouteId.containsKey(routeId);
    }

    /**
     * Find the corresponding service for a routeId
     */
    public HttpGatewayRemoteService findService(String routeId) {
        return servicesByRouteId.get(routeId);
    }

    /**
     * Find the corresponding service of a <code>MatchingRoute</code> object that is returned
     * by the router service: <code>HttpGatewayRouter</code>
     */
    public String findServiceBaseUrl(MatchingRoute matchingRoute) {
        HttpGatewayRemoteService remoteService = findService(matchingRoute.getMatchingEndpoint().getHttpEndpoint().getRouteId());
        if (remoteService != null) {
            return remoteService.getBaseUrl();
        }
        return null;
    }
}
