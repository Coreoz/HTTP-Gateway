package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remoteservices.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServices {
    static final String CONFIG_SERVICE_ID = "service-id";

    public static HttpGatewayRemoteServicesIndex readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServicesIndex readConfig(Config gatewayConfig) {
        List<HttpGatewayRemoteService> remoteServices = readRemoteServices(gatewayConfig);
        return new HttpGatewayRemoteServicesIndex(
            remoteServices,
            readRewriteRoutes(
                remoteServices
                    .stream()
                    .flatMap(service -> service.getRoutes().stream().map(HttpGatewayRemoteServiceRoute::getRouteId))
                    .collect(Collectors.toSet()),
                gatewayConfig
            )
        );
    }

    static List<? extends Config> readRemoteServicesConfig(Config gatewayConfig) {
        return gatewayConfig.getConfigList("remote-services");
    }

    public static List<HttpGatewayRemoteService> readRemoteServices(Config gatewayConfig) {
        return readRemoteServicesConfig(gatewayConfig)
            .stream()
            .map(serviceConfig -> new HttpGatewayRemoteService(
                serviceConfig.getString(CONFIG_SERVICE_ID),
                serviceConfig.getString("base-url"),
                serviceConfig.getConfigList("routes").stream().map(routeConfig -> new HttpGatewayRemoteServiceRoute(
                    routeConfig.getString("route-id"),
                    routeConfig.getString("method"),
                    routeConfig.getString("path")
                )).collect(Collectors.toList())
            ))
            .peek(HttpGatewayConfigRemoteServices::validatePathStartsWithSlash)
            .collect(Collectors.toList());
    }

    private static void validatePathStartsWithSlash(HttpGatewayRemoteService remoteService) {
        for (HttpGatewayRemoteServiceRoute route : remoteService.getRoutes()) {
            if (!route.getPath().startsWith("/")) {
                throw new HttpGatewayConfigException(
                    "Route path '" + route.getPath() + "' must start with a / in '" + route.getRouteId() + "' in service '" + remoteService.getServiceId() + "'"
                );
            }
        }
    }

    public static List<HttpGatewayRewriteRoute> readRewriteRoutes(Set<String> existingRoutesIds, Config gatewayConfig) {
        return gatewayConfig
            .getConfigList("gateway-rewrite-routes")
            .stream()
            .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                rewriteRouteConfig.getString("gateway-path"),
                rewriteRouteConfig.getString("route-id")
            ))
            .peek(routeConfig -> {
                if(!existingRoutesIds.contains(routeConfig.getRouteId())) {
                    throw new HttpGatewayConfigException(
                        "Rewrite route with gateway-path=" + routeConfig.getGatewayPath() + " references a route-id that does not exist: " + routeConfig.getRouteId()
                    );
                }
            })
            .collect(Collectors.toList());
    }
}
