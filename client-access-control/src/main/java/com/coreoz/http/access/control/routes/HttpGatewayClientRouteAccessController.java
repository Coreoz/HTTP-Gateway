package com.coreoz.http.access.control.routes;

public interface HttpGatewayClientRouteAccessController {
    /**
     * Verify that a client can access a route inside a service
     */
    boolean hasAccess(String clientId, String routeId, String serviceId);
}
