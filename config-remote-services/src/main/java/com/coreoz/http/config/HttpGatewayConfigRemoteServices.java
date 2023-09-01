package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remoteservices.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServices {
    public static HttpGatewayRemoteServicesIndex readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServicesIndex readConfig(Config gatewayConfig) {
        return new HttpGatewayRemoteServicesIndex(
            readRemoteServices(gatewayConfig),
            readRewriteRoutes(gatewayConfig)
        );
    }

    public static List<HttpGatewayRemoteService> readRemoteServices(Config gatewayConfig) {
        return gatewayConfig
            .getConfigList("remote-services")
            .stream()
            .map(serviceConfig -> new HttpGatewayRemoteService(
                serviceConfig.getString("service-id"),
                serviceConfig.getString("base-url"),
                serviceConfig.getConfigList("routes").stream().map(routeConfig -> new HttpGatewayRemoteServiceRoute(
                    routeConfig.getString("route-id"),
                    routeConfig.getString("method"),
                    routeConfig.getString("path")
                )).collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
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
