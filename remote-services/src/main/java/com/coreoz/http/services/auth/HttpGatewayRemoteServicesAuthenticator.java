package com.coreoz.http.services.auth;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;

import java.util.Map;

/**
 * Index and expose service and route authenticator for upstream services
 */
public class HttpGatewayRemoteServicesAuthenticator {
    private final Map<String, HttpGatewayUpstreamAuthenticator> servicesAuthenticators;
    private final Map<String, HttpGatewayUpstreamAuthenticator> routesAuthenticators;

    public HttpGatewayRemoteServicesAuthenticator(
        Map<String, HttpGatewayUpstreamAuthenticator> servicesAuthenticators,
        Map<String, HttpGatewayUpstreamAuthenticator> routesAuthenticators
    ) {
        this.servicesAuthenticators = servicesAuthenticators;
        this.routesAuthenticators = routesAuthenticators;
    }

    /**
     * Fetch the authenticator for a service and a route
     */
    public HttpGatewayUpstreamAuthenticator forRoute(String serviceId, String routeId) {
        HttpGatewayUpstreamAuthenticator routeAuthenticator = routesAuthenticators.get(routeId);
        if (routeAuthenticator != null) {
            return routeAuthenticator;
        }
        return servicesAuthenticators.get(serviceId);
    }
}
