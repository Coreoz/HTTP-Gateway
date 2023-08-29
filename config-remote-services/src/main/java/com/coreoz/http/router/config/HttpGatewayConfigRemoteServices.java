package com.coreoz.http.router.config;

import com.coreoz.http.access.control.HttpGatewayRemoteService;
import com.coreoz.http.access.control.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.access.control.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.access.control.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServices {
    public static HttpGatewayRemoteServicesIndex readConfig(Config baseConfig) {
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
                serviceConfig.getString("serviceId"),
                serviceConfig.getString("base-url"),
                serviceConfig.getConfigList("routes").stream().map(routeConfig -> new HttpGatewayRemoteServiceRoute(
                    routeConfig.getString("routeId"),
                    routeConfig.getString("method"),
                    routeConfig.getString("path")
                )).collect(Collectors.toList())
            ))
            .collect(Collectors.toList());
    }

    public static List<HttpGatewayRewriteRoute> readRewriteRoutes(Config baseConfig) {
        return baseConfig
            .getConfigList("http-gateway.gateway-rewrite-routes")
            .stream()
            .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                rewriteRouteConfig.getString("gateway-path"),
                rewriteRouteConfig.getString("routeId")
            ))
            .collect(Collectors.toList());
    }
}
