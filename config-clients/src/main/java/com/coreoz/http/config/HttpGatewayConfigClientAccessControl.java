package com.coreoz.http.config;

import com.coreoz.http.access.control.HttpGatewayClientAccessController;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.routes.HttpGatewayClientRouteAccessControl;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
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

    /**
     * Create a new {@link HttpGatewayConfigClientAccessControl} instance.
     * Note that all clients authentication methods will be supported. This is good for testing, but in production,
     * it is recommended to have one a just a few supported authentication methods to reduce attack risks:
     * see {@link #readConfig(HttpGatewayConfigLoader, List)}
     * @param configLoader The instance of the config
     * @return The new instance of {@link HttpGatewayConfigClientAccessControl}
     */
    public static HttpGatewayConfigClientAccessControl readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    /**
     * Create a new {@link HttpGatewayConfigClientAccessControl} instance using only some supported authentication methods
     * @param configLoader The instance of the config loader
     * @param supportedAuthConfigs Available auth configuration are listed in {@link HttpGatewayConfigClientAuth}
     * @return The new instance of {@link HttpGatewayConfigClientAccessControl}
     */
    public static HttpGatewayConfigClientAccessControl readConfig(HttpGatewayConfigLoader configLoader, List<HttpGatewayConfigClientAuth.HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        return readConfig(configLoader.getHttpGatewayConfig(), supportedAuthConfigs);
    }

    /**
     * Create a new {@link HttpGatewayConfigClientAccessControl} instance using a config object.
     * Note that all clients authentication methods will be supported. This is good for testing, but in production,
     * it is recommended to have one a just a few supported authentication methods to reduce attack risks:
     * see {@link #readConfig(Config, List)}
     * @param gatewayConfig The config object containing clients configuration
     * @return The new instance of {@link HttpGatewayConfigClientAccessControl}
     */
    public static HttpGatewayConfigClientAccessControl readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, HttpGatewayConfigClientAuth.supportedAuthConfigs());
    }

    /**
     * Create a new {@link HttpGatewayConfigClientAccessControl} instance using only some supported authentication methods
     * @param gatewayConfig The config object containing clients configuration
     * @param supportedAuthConfigs Available auth configuration are listed in {@link HttpGatewayConfigClientAuth}
     * @return The new instance of {@link HttpGatewayConfigClientAccessControl}
     */
    public static HttpGatewayConfigClientAccessControl readConfig(Config gatewayConfig, List<HttpGatewayConfigClientAuth.HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        List<? extends Config> clientConfigs = gatewayConfig.getConfigList("clients");
        HttpGatewayClientAuthenticator authenticator = HttpGatewayConfigClientAuth.readAuth(clientConfigs, supportedAuthConfigs);
        HttpGatewayClientRouteAccessControl routeAccessControl = HttpGatewayConfigClientRoutes.readClientsRoutes(gatewayConfig, clientConfigs);
        return new HttpGatewayConfigClientAccessControl(authenticator, routeAccessControl);
    }

    @Override
    public String authenticate(Http.Request downstreamRequest) {
        return authenticator.authenticate(downstreamRequest);
    }

    @Override
    public boolean hasAccess(String clientId, String routeId, String serviceId) {
        return routeAccessControl.hasAccess(clientId, routeId, serviceId);
    }

    public HttpGatewayConfigClientAccessControl validateConfig(HttpGatewayRemoteServicesIndex remoteServicesIndex) {
        routeAccessControl.validateConfig(remoteServicesIndex);
        return this;
    }
}
