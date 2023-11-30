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
                routeGroupConfig.getString("routes-group-id"),
                routeGroupConfig.getStringList("routes")
            ))
            .collect(Collectors.toList());
    }

    public static List<HttpGatewayClientRoutesControl> readClientRoutesControls(List<? extends Config> clientConfigs) {
        return clientConfigs
            .stream()
            .map(clientConfig -> new HttpGatewayClientRoutesControl(
                clientConfig.getString("client-id"),
                getConfigStringListOrEmpty(clientConfig, "allowed-routes"),
                getConfigStringListOrEmpty(clientConfig, "allowed-routes-groups"),
                getConfigStringListOrEmpty(clientConfig, "allowed-services")
            ))
            .collect(Collectors.toList());
    }

    private static List<String> getConfigStringListOrEmpty(Config config, String configPath) {
        if (config.hasPath(configPath)) {
            return config.getStringList(configPath);
        }
        return List.of();
    }
}
