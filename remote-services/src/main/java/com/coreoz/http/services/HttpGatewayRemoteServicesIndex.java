package com.coreoz.http.services;

import com.coreoz.http.exception.HttpGatewayValidationException;
import com.coreoz.http.router.HttpGatewayRoute;
import com.coreoz.http.routes.HttpRoutes;
import com.coreoz.http.routes.parsing.ParsedSegment;
import com.coreoz.http.routes.router.HttpRoute;
import com.coreoz.http.routes.router.index.IndexRouteLeaf;
import com.coreoz.http.routes.router.index.IndexedRoutes;
import com.coreoz.http.routes.router.index.SearchRouteIndexer;
import com.coreoz.http.routes.router.search.MatchingRoute;
import org.jetbrains.annotations.NotNull;

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
            .flatMap(service -> service.routes().stream().map(route -> Map.entry(route.routeId(), service)))
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
                HttpGatewayRewriteRoute::routeId,
                HttpGatewayRewriteRoute::downstreamPath
            ));
    }

    /**
     * Returns the available services
     */
    public List<HttpGatewayRemoteService> getServices() {
        return services;
    }

    private Stream<HttpGatewayRoute> routesStream() {
        return services
            .stream()
            .flatMap(service -> service.routes().stream().map(this::serviceRouteToHttpRoute));
    }

    /**
     * Generate the HTTP Gateway {@link HttpRoute} for a remote service route.
     * This method takes into consideration route rewriting.
     * @param route The remote service route
     */
    public @NotNull HttpGatewayRoute serviceRouteToHttpRoute(@NotNull HttpGatewayRemoteServiceRoute route) {
        return new HttpGatewayRoute(
            route.routeId(),
            route.method(),
            gatewayRewriteRoutes.getOrDefault(route.routeId(), route.path()),
            parseUpstreamPath(route)
        );
    }

    private static @NotNull List<ParsedSegment> parseUpstreamPath(HttpGatewayRemoteServiceRoute route) {
        if (route.path() == null) {
            throw new HttpGatewayValidationException(
                "Upstream path cannot be null for route '" + route.routeId() + "'"
            );
        }

        return HttpRoutes.parsePathAsSegments(route.path());
    }

    /**
     * Computes the route as a <code>Iterable<HttpEndpoint></code> object that can be used
     * in the router <code>HttpGatewayRouter</code> class
     */
    public Iterable<HttpGatewayRoute> computeRoutes() {
        return routesStream()::iterator;
    }

    /**
     * Does the same as {@link #computeRoutes()} but validate the routes consistency at the same time.
     * @throws HttpGatewayValidationException If there is an inconsistency detected
     */
    public Map<String, IndexedRoutes<HttpGatewayRoute>> computeValidatedIndexedRoutes() {
        validateRewriteRoutes();

        Map<String, IndexedRoutes<HttpGatewayRoute>> indexedEndpoints = new HashMap<>();
        routesStream().forEach(route -> {
            if (route.downstreamPath() == null) {
                throw new HttpGatewayValidationException(
                    "Downstream path cannot be null for route '" + route.routeId() + "'"
                );
            }

            if (!route.downstreamPath().startsWith("/")) {
                throw new HttpGatewayValidationException(
                    "Downstream path '" + route.downstreamPath() + "' must start with a / for route '" + route.routeId() + "'"
                );
            }
            @NotNull IndexRouteLeaf<HttpGatewayRoute> addedRoute = SearchRouteIndexer.addRouteToIndex(indexedEndpoints, route);
            if (addedRoute.httpRoute() != route) {
                throw new HttpGatewayValidationException(
                    "Duplicate downstream path for routes '"
                    + addedRoute.httpRoute().routeId()
                    + "' and '"+route.routeId()+"': the endpoint "
                    + route + " overlaps the path of the route " + addedRoute.httpRoute()
                );
            }
        });
        return indexedEndpoints;
    }

    private void validateRewriteRoutes() {
        Map<String, HttpGatewayRemoteServiceRoute> indexedRoutesById = services
            .stream()
            .flatMap(service -> service.routes().stream())
            .collect(Collectors.toMap(
                HttpGatewayRemoteServiceRoute::routeId,
                Function.identity()
            ));
        for (Map.Entry<String, String> rewriteRouteEntry : gatewayRewriteRoutes.entrySet()) {
            HttpGatewayRemoteServiceRoute initialRoute = indexedRoutesById.get(rewriteRouteEntry.getKey());
            if (initialRoute == null) {
                throw new HttpGatewayValidationException("Incorrect rewrite route that references a non existing routeId: " + rewriteRouteEntry.getKey());
            }

            String rewriteRoutePath = gatewayRewriteRoutes.get(rewriteRouteEntry.getKey());
            if (!areRouteCompatible(initialRoute.path(), rewriteRoutePath)) {
                throw new HttpGatewayValidationException("Incorrect rewrite route for routeId='" + rewriteRouteEntry.getKey() +
                    "', rewrite route path does not match initial route path, the paths pattern (e.g. {pattern}) names must be identical. Received: "
                    + initialRoute.path() + " (initial) != " + rewriteRoutePath + " (rewrite)");
            }
        }
    }

    private boolean areRouteCompatible(String initialPath, String rewritePath) {
        return extractPatternNames(HttpRoutes.parsePathAsSegments(initialPath))
            .equals(extractPatternNames(HttpRoutes.parsePathAsSegments(rewritePath)));
    }

    private Set<String> extractPatternNames(List<ParsedSegment> routeSegments) {
        return routeSegments
            .stream()
            .filter(ParsedSegment::isPattern)
            .map(ParsedSegment::name)
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
    public String findServiceBaseUrl(MatchingRoute<HttpGatewayRoute> matchingRoute) {
        HttpGatewayRemoteService remoteService = findService(matchingRoute.httpRoute().routeId());
        if (remoteService != null) {
            return remoteService.baseUrl();
        }
        return null;
    }
}
