package com.coreoz.http.services.auth;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Index and expose service and route authenticator for upstream services
 */
public class HttpGatewayRemoteServiceAuthenticator {
    private final Map<String, HttpGatewayUpstreamAuthenticator> servicesAuthenticators;
    private final Map<String, HttpGatewayUpstreamAuthenticator> routesAuthenticators;

    public HttpGatewayRemoteServiceAuthenticator(
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

    /**
     * Index services authenticators
     */
    public static HttpGatewayRemoteServiceAuthenticator fromRemoteClientAuthentications(List<HttpGatewayRemoteServiceAuth> servicesAuth) {
        return new HttpGatewayRemoteServiceAuthenticator(
            servicesAuth.stream().collect(Collectors.toMap(
                HttpGatewayRemoteServiceAuth::getServiceId,
                HttpGatewayRemoteServiceAuth::getAuthenticator
            )),
            Map.of()
        );
    }
}
