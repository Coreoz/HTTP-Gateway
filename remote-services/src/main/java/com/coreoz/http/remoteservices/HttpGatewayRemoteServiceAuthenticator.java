package com.coreoz.http.remoteservices;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpGatewayRemoteServiceAuthenticator {
    private final Map<String, HttpGatewayUpstreamAuthenticator> servicesAuthenticators;
    private final Map<String, HttpGatewayUpstreamAuthenticator> routesAuthenticators;

    public HttpGatewayRemoteServiceAuthenticator(Map<String, HttpGatewayUpstreamAuthenticator> servicesAuthenticators, Map<String, HttpGatewayUpstreamAuthenticator> routesAuthenticators) {
        this.servicesAuthenticators = servicesAuthenticators;
        this.routesAuthenticators = routesAuthenticators;
    }

    public HttpGatewayUpstreamAuthenticator forRoute(String remoteServiceId, String routeId) {
        HttpGatewayUpstreamAuthenticator routeAuthenticator = routesAuthenticators.get(routeId);
        if (routeAuthenticator != null) {
            return routeAuthenticator;
        }
        return servicesAuthenticators.get(remoteServiceId);
    }

    public static HttpGatewayRemoteServiceAuthenticator fromRemoteClientAuthentications(List<HttpGatewayRemoteServiceAuth> clientsAuth) {
        return new HttpGatewayRemoteServiceAuthenticator(
            clientsAuth.stream().collect(Collectors.toMap(
                HttpGatewayRemoteServiceAuth::getServiceId,
                HttpGatewayRemoteServiceAuth::getAuthenticator
            )),
            Map.of()
        );
    }
}
