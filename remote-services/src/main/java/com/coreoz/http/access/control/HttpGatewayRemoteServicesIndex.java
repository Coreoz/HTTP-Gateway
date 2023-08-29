package com.coreoz.http.access.control;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.MatchingRoute;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Index remote services routes so it can be used in a HttpGatewayRouter
 */
public class HttpGatewayRemoteServicesIndex {
    private final Map<String, HttpGatewayRemoteService> servicesByRouteId;
    private final List<HttpGatewayRemoteService> services;
    private final Map<String, String> gatewayRewriteRoutes;

    // TODO provide a way to validate all the config
    public HttpGatewayRemoteServicesIndex(List<HttpGatewayRemoteService> services, List<HttpGatewayRewriteRoute> rewriteRoutes) {
        this.services = services;
        this.servicesByRouteId = services
            .stream()
            .flatMap(service -> service.getRoutes().stream().map(route -> Map.entry(route.getRouteId(), service)))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
        this.gatewayRewriteRoutes = rewriteRoutes
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRewriteRoute::getRouteId,
                HttpGatewayRewriteRoute::getGatewayPath
            ));
    }

    public List<HttpGatewayRemoteService> getServices() {
        return services;
    }

    public Iterable<HttpEndpoint> getRoutes() {
        return services
            .stream()
            .flatMap(service -> service.getRoutes().stream().map(route -> new HttpEndpoint(
                route.getRouteId(),
                route.getMethod(),
                route.getPath(),
                gatewayRewriteRoutes.getOrDefault(route.getRouteId(), route.getPath())
            )))
            ::iterator;
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
