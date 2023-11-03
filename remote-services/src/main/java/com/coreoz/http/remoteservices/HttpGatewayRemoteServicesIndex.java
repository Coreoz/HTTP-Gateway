package com.coreoz.http.remoteservices;

import com.coreoz.http.router.SearchRouteIndexer;
import com.coreoz.http.router.data.*;
import com.coreoz.http.validation.HttpGatewayConfigException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Index remote services routes so it can be used in a HttpGatewayRouter
 */
public class HttpGatewayRemoteServicesIndex {
    private final Map<String, HttpGatewayRemoteService> servicesByRouteId;
    private final List<HttpGatewayRemoteService> services;
    private final Map<String, String> gatewayRewriteRoutes;

    public HttpGatewayRemoteServicesIndex(List<HttpGatewayRemoteService> services, List<HttpGatewayRewriteRoute> rewriteRoutes) {
        this.services = services;
        this.servicesByRouteId = services
            .stream()
            .flatMap(service -> service.getRoutes().stream().map(route -> Map.entry(route.getRouteId(), service)))
            .collect(Collector.of(
                HashMap::new,
                (map, routeEntry) -> {
                    if (map.put(routeEntry.getKey(), routeEntry.getValue()) != null) {
                        throw new HttpGatewayConfigException("Duplicate route-id for route '"+routeEntry.getKey()+"'");
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

    public Iterable<HttpEndpoint> computeRoutes() {
        return routesStream()::iterator;
    }

    public Map<String, IndexedEndpoints> computeValidatedIndexedRoutes() {
        validateRewriteRoutes();

        Map<String, IndexedEndpoints> indexedEndpoints = new HashMap<>();
        routesStream().forEach(endpoint -> {
            if (endpoint.getDownstreamPath() == null) {
                throw new HttpGatewayConfigException(
                    "Downstream path cannot be null for route '" + endpoint.getRouteId() + "'"
                );
            }
            if (!endpoint.getDownstreamPath().startsWith("/")) {
                throw new HttpGatewayConfigException(
                    "Downstream path '" + endpoint.getDownstreamPath() + "' must start with a / for route '" + endpoint.getRouteId() + "'"
                );
            }

            if (endpoint.getUpstreamPath() == null) {
                throw new HttpGatewayConfigException("Upstream path must not be null for route '" + endpoint.getRouteId());
            }

            EndpointParsedData addedEndpoint = SearchRouteIndexer.addEndpointToIndex(indexedEndpoints, endpoint);
            if (addedEndpoint == null) {
                throw new HttpGatewayConfigException("Error reading endpoint " + endpoint);
            }
            if (addedEndpoint.getHttpEndpoint() != endpoint) {
                throw new HttpGatewayConfigException(
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
        for (String routeId : gatewayRewriteRoutes.keySet()) {
            HttpGatewayRemoteServiceRoute initialRoute = indexedRoutesById.get(routeId);
            if (initialRoute == null) {
                throw new HttpGatewayConfigException("Incorrect rewrite route that references a non existing routeId: " + routeId);
            }

            String rewriteRoutePath = gatewayRewriteRoutes.get(routeId);
            if (!areRouteCompatible(initialRoute.getPath(), rewriteRoutePath)) {
                throw new HttpGatewayConfigException("Incorrect rewrite route for routeId='" + routeId +
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

    public HttpGatewayRemoteService findService(String routeId) {
        return servicesByRouteId.get(routeId);
    }

    public String findServiceBaseUrl(MatchingRoute matchingRoute) {
        HttpGatewayRemoteService remoteService = findService(matchingRoute.getMatchingEndpoint().getHttpEndpoint().getRouteId());
        if (remoteService != null) {
            return remoteService.getBaseUrl();
        }
        return null;
    }
}
