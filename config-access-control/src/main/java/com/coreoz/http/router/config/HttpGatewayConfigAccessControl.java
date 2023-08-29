package com.coreoz.http.router.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthApiKey;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.coreoz.http.remote.services.HttpGatewayRemoteService;
import com.coreoz.http.remote.services.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remote.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remote.services.HttpGatewayRewriteRoute;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigAccessControl {
    private final HttpGatewayAuthenticator authenticator;
    private final HttpGatewayClientRouteAccessControl routeAccessControl;

    public static HttpGatewayConfigAccessControl readConfig(Config baseConfig) {
        List<? extends Config> clientConfigs = baseConfig.getConfigList("http-gateway.clients");
        HttpGatewayAuthenticator authenticator = HttpGatewayConfigClientAuth.readAuth(clientConfigs);
        HttpGatewayClientRouteAccessControl routeAccessControl = TODO;
    }

    public static List<HttpGatewayAuthenticator> readAuth(Config baseConfig) {
        return
            .stream()
            .map(clientConfig -> new HttpGatewayRemoteService(
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
            .getConfigList("http-gateway.gateway-rewrite-routes")
            .stream()
            .map(rewriteRouteConfig -> new HttpGatewayRewriteRoute(
                rewriteRouteConfig.getString("gateway-path"),
                rewriteRouteConfig.getString("routeId")
            ))
            .collect(Collectors.toList());
    }

}
