package com.coreoz.http.access.control.routes;

import com.coreoz.http.access.control.auth.HttpGatewayAuthenticator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpGatewayClientRouteAccessControl {
    private final Map<String, Set<String>> allowedRoutesByClient;
    private final Map<String, Set<String>> allowedServicesByClient;

    public HttpGatewayClientRouteAccessControl(
        List<HttpGatewayRoutesGroup> routesGroups,
        List<HttpGatewayClientRoutesControl> clientRoutesControls
        ) {
        Map<String, List<String>> routesGroupsIndex = routesGroups
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRoutesGroup::getRoutesGroupId,
                HttpGatewayRoutesGroup::getRouteIds
            ));
        this.allowedRoutesByClient = clientRoutesControls
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayClientRoutesControl::getClientId,
                client -> Stream
                    .concat(
                        client.getRestrictedRoutes().stream(),
                        client
                            .getRestrictedRoutesGroups()
                            .stream()
                            .flatMap(routesGroupId -> routesGroupsIndex.get(routesGroupId).stream())
                    )
                    .collect(Collectors.toSet())
            ));
        this.allowedServicesByClient = clientRoutesControls
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayClientRoutesControl::getClientId,
                client -> Set.copyOf(client.getRestrictedServices())
            ));
    }

    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return allowedRoutesByClient.getOrDefault(clientId, Set.of()).contains(routeId)
            || allowedServicesByClient.getOrDefault(clientId, Set.of()).contains(serviceId);
    }
}
