package com.coreoz.http.config;

import com.coreoz.http.access.control.HttpGatewayClientAccessController;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.typesafe.config.Config;
import play.mvc.Http;

import java.util.List;

/**
 * Handle Gateway client authorization and access control verification
 */
public class HttpGatewayConfigClientAccessControl implements HttpGatewayClientAccessController {
    private final HttpGatewayClientAuthenticator authenticator;
    private final HttpGatewayClientRouteAccessControl routeAccessControl;

    private HttpGatewayConfigClientAccessControl(HttpGatewayClientAuthenticator authenticator, HttpGatewayClientRouteAccessControl routeAccessControl) {
        this.authenticator = authenticator;
        this.routeAccessControl = routeAccessControl;
    }

    public static HttpGatewayConfigClientAccessControl readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayConfigClientAccessControl readConfig(Config gatewayConfig) {
        List<? extends Config> clientConfigs = gatewayConfig.getConfigList("clients");
        HttpGatewayClientAuthenticator authenticator = HttpGatewayConfigClientAuth.readAuth(clientConfigs);
        HttpGatewayClientRouteAccessControl routeAccessControl = HttpGatewayConfigClientRoutes.readClientsRoutes(gatewayConfig, clientConfigs);
        return new HttpGatewayConfigClientAccessControl(authenticator, routeAccessControl);
    }

    @Override
    public String authenticate(Http.Request downstreamRequest) {
        return authenticator.authenticate(downstreamRequest);
    }

    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return routeAccessControl.hasAccess(clientId, routeId, serviceId);
    }

    public HttpGatewayConfigClientAccessControl validateConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        routeAccessControl.validateConfig(remoteServicesIndex);
        return this;
    }
}
