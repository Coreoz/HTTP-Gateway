package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthenticator;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.typesafe.config.Config;
import play.mvc.Http;

import java.util.List;

public class HttpGatewayConfigAccessControl {
    private final HttpGatewayAuthenticator authenticator;
    private final HttpGatewayClientRouteAccessControl routeAccessControl;

    private HttpGatewayConfigAccessControl(HttpGatewayAuthenticator authenticator, HttpGatewayClientRouteAccessControl routeAccessControl) {
        this.authenticator = authenticator;
        this.routeAccessControl = routeAccessControl;
    }

    public static HttpGatewayConfigAccessControl readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayConfigAccessControl readConfig(Config gatewayConfig) {
        List<? extends Config> clientConfigs = gatewayConfig.getConfigList("clients");
        HttpGatewayAuthenticator authenticator = HttpGatewayConfigClientAuth.readAuth(clientConfigs);
        // TODO implémenter la lecture de la conf de gestion des accès sur le routing
        HttpGatewayClientRouteAccessControl routeAccessControl = HttpGatewayConfigClientRoutes.readClientsRoutes(gatewayConfig, clientConfigs);
        return new HttpGatewayConfigAccessControl(authenticator, routeAccessControl);
    }

    public String authenticate(Http.Request downstreamRequest) {
        return authenticator.authenticate(downstreamRequest);
    }

    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return routeAccessControl.hasAccess(clientId, routeId, serviceId);
    }
}
