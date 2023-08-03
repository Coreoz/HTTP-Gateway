package com.coreoz.http.remote.services;

import com.coreoz.http.router.data.HttpEndpoint;

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
                gatewayRewriteRoutes.getOrDefault(route.getRouteId(), route.getPath()),
                service.getBaseUrl()
            )))
            ::iterator;
    }

    public HttpGatewayRemoteService findService(String routeId) {
        return servicesByRouteId.get(routeId);
    }
}
