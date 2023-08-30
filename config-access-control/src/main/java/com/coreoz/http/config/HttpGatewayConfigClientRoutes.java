package com.coreoz.http.config;

import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.coreoz.http.access.control.routes.HttpGatewayClientRoutesControl;
import com.coreoz.http.access.control.routes.HttpGatewayRoutesGroup;
import com.typesafe.config.Config;

import java.util.List;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientRoutes {
    public static HttpGatewayClientRouteAccessControl readClientsRoutes(Config gatewayConfig, List<? extends Config> clientConfigs) {
        return new HttpGatewayClientRouteAccessControl(
            readRoutesGroups(gatewayConfig),
            readClientRoutesControls(clientConfigs)
        );
    }

    public static List<HttpGatewayRoutesGroup> readRoutesGroups(Config gatewayConfig) {
        return gatewayConfig
            .getConfigList("routes-groups")
            .stream()
            .map(routeGroupConfig -> new HttpGatewayRoutesGroup(
                routeGroupConfig.getString("routesGroupId"),
                routeGroupConfig.getStringList("routes")
            ))
            .collect(Collectors.toList());
    }

    public static List<HttpGatewayClientRoutesControl> readClientRoutesControls(List<? extends Config> clientConfigs) {
        return clientConfigs
            .stream()
            .map(clientConfig -> new HttpGatewayClientRoutesControl(
                clientConfig.getString("clientId"),
                clientConfig.getStringList("restricted-routes"),
                clientConfig.getStringList("restricted-routes-groups"),
                clientConfig.getStringList("restricted-services")
            ))
            .collect(Collectors.toList());
    }
}
