package com.coreoz.http.router.config;

import com.coreoz.http.remote.services.HttpGatewayRemoteService;
import com.coreoz.http.remote.services.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remote.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remote.services.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServices {
    public static HttpGatewayRemoteServicesIndex indexRemoteServices(Config baseConfig) {
        return new HttpGatewayRemoteServicesIndex(
            readRemoteServices(baseConfig),
            readRewriteRoutes(baseConfig)
        );
    }

    public static List<HttpGatewayRemoteService> readRemoteServices(Config baseConfig) {
        return baseConfig
            .getConfigList("http-gateway.remote-services")
            .stream()
            .map(serviceConfig -> new HttpGatewayRemoteService(
                serviceConfig.getString("id"),
                serviceConfig.getString("base-url"),
                serviceConfig.getConfigList("routes").stream().map(routeConfig -> new HttpGatewayRemoteServiceRoute(
                    routeConfig.getString("id"),
                    routeConfig.getString("method"),
                    routeConfig.getString("path")
                )).collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }

    public static List<HttpGatewayRewriteRoute> readRewriteRoutes(Config baseConfig) {
        return baseConfig
            .getConfigList("http-gateway.remote-services")
            .stream()
            .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                rewriteRouteConfig.getString("gateway-path"),
                rewriteRouteConfig.getString("routeId")
            ))
            .collect(Collectors.toList());
    }
}
