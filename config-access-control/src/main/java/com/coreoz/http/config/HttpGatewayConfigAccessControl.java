package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.typesafe.config.Config;
import play.mvc.Http;

import java.util.List;

public class HttpGatewayConfigAccessControl {
    private final HttpGatewayClientAuthenticator authenticator;
    private final HttpGatewayClientRouteAccessControl routeAccessControl;

    private HttpGatewayConfigAccessControl(HttpGatewayClientAuthenticator authenticator, HttpGatewayClientRouteAccessControl routeAccessControl) {
        this.authenticator = authenticator;
        this.routeAccessControl = routeAccessControl;
    }

    public static HttpGatewayConfigAccessControl readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayConfigAccessControl readConfig(Config gatewayConfig) {
        List<? extends Config> clientConfigs = gatewayConfig.getConfigList("clients");
        HttpGatewayClientAuthenticator authenticator = HttpGatewayConfigClientAuth.readAuth(clientConfigs);
        HttpGatewayClientRouteAccessControl routeAccessControl = HttpGatewayConfigClientRoutes.readClientsRoutes(gatewayConfig, clientConfigs);
        return new HttpGatewayConfigAccessControl(authenticator, routeAccessControl);
    }

    public String authenticate(Http.Request downstreamRequest) {
        return authenticator.authenticate(downstreamRequest);
    }

    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return routeAccessControl.hasAccess(clientId, routeId, serviceId);
    }

    public HttpGatewayConfigAccessControl validateConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        routeAccessControl.validateConfig(remoteServicesIndex);
        return this;
    }
}
