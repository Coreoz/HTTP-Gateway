package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remoteservices.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServices {
    static final String CONFIG_SERVICE_ID = "service-id";

    public static HttpGatewayRemoteServicesIndex readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServicesIndex readConfig(Config gatewayConfig) {
        return new HttpGatewayRemoteServicesIndex(
            readRemoteServices(gatewayConfig),
            readRewriteRoutes(gatewayConfig)
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

    public static List<HttpGatewayRewriteRoute> readRewriteRoutes(Config gatewayConfig) {
        return gatewayConfig
            .getConfigList("gateway-rewrite-routes")
            .stream()
            .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                rewriteRouteConfig.getString("gateway-path"),
                rewriteRouteConfig.getString("route-id")
            ))
            .collect(Collectors.toList());
    }
}
