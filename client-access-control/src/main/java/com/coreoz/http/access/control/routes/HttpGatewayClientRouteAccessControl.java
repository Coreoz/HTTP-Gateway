package com.coreoz.http.access.control.routes;

import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.exception.HttpGatewayValidationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpGatewayClientRouteAccessControl implements HttpGatewayClientRouteAccessController {
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
                        client.getAllowedRoutes().stream(),
                        client
                            .getAllowedRoutesGroups()
                            .stream()
                            .flatMap(routesGroupId -> Optional.ofNullable(routesGroupsIndex.get(routesGroupId))
                                .map(List::stream)
                                .orElseThrow(() -> new HttpGatewayValidationException(
                                    "Route group '"+routesGroupId +"' does not exist in available routes groups: " + routesGroupsIndex.keySet()
                                ))
                            )
                    )
                    .collect(Collectors.toSet())
            ));
        this.allowedServicesByClient = clientRoutesControls
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayClientRoutesControl::getClientId,
                client -> Set.copyOf(client.getAllowedServices())
            ));
    }

    public void validateConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        validateServiceConfig(remoteServicesIndex);
        validateRoutesConfig(remoteServicesIndex);
    }

    private void validateRoutesConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        for (Map.Entry<String, Set<String>> routesByClient : allowedRoutesByClient.entrySet()) {
            for (String routeId : routesByClient.getValue()) {
                if (!remoteServicesIndex.hasRoute(routeId)) {
                    throw new HttpGatewayValidationException(
                        "Route ID '" + routeId + "' is not recognized in client ID '" + routesByClient.getKey() + "'."
                    );
                }
            }
        }
    }

    private void validateServiceConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        Set<String> remoteServiceIds = remoteServicesIndex
            .getServices()
            .stream()
            .map(HttpGatewayRemoteService::getServiceId)
            .collect(Collectors.toSet());
        for (Map.Entry<String, Set<String>> serviceByClient : allowedServicesByClient.entrySet()) {
            for (String serviceId : serviceByClient.getValue()) {
                if (!remoteServiceIds.contains(serviceId)) {
                    throw new HttpGatewayValidationException(
                        "Service ID '" + serviceId + "' is not recognized in client ID '" + serviceByClient.getKey()
                        + "'. Available service ID: " + remoteServiceIds
                    );
                }
            }
        }
    }

    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return allowedRoutesByClient.getOrDefault(clientId, Set.of()).contains(routeId)
            || allowedServicesByClient.getOrDefault(clientId, Set.of()).contains(serviceId);
    }
}
