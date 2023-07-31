package com.coreoz.http.router.config;

import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.router.data.HttpEndpoint;
import com.typesafe.config.Config;

public class HttpGatewayRouterConfig {
    // TODO do not return HttpGatewayRouter directly, just return indexed routes by id and services
    // TODO then this object can be changed to a list of HttpEndpoint
    public static <T> HttpGatewayRouter<T> readConfig(Config baseConfig) {
        return new HttpGatewayRouter<>(
            baseConfig
                .getConfigList("http-gateway.remote-services")
                .stream()
                // TODO parse service
                .flatMap(serviceConfig -> serviceConfig.getConfigList("routes").stream().map(routeConfig -> new HttpEndpoint.of(
                    routeConfig.getString("id"),
                    routeConfig.getString("method"),
                    routeConfig.getString("method"),
                    )))
                ::iterator
        );
    }
}
