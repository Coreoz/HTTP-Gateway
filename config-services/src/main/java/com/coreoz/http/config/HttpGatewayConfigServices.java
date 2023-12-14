package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remoteservices.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpGatewayConfigServices {
    static final String CONFIG_SERVICE_ID = "service-id";
    static final String CONFIG_REWRITE_ROUTES = "gateway-rewrite-routes";

    public static HttpGatewayRemoteServicesIndex readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServicesIndex readConfig(Config gatewayConfig) {
        return createRemoteServicesIndex(gatewayConfig);
    }

    private static HttpGatewayRemoteServicesIndex createRemoteServicesIndex(Config gatewayConfig) {
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
            .collect(Collectors.toList());
    }

    public static List<HttpGatewayRewriteRoute> readRewriteRoutes(Set<String> existingRoutesIds, Config gatewayConfig) {
        if (gatewayConfig.hasPath(CONFIG_REWRITE_ROUTES)) {
            return gatewayConfig
                .getConfigList("gateway-rewrite-routes")
                .stream()
                .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                    rewriteRouteConfig.getString("route-id"),
                    rewriteRouteConfig.getString("downstream-path")
                ))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
