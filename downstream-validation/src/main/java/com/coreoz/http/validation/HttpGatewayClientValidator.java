package com.coreoz.http.validation;

import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.data.DestinationRoute;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.mvc.Http;

/**
 * Validate authorized clients from incoming downstream requests
 */
public class HttpGatewayClientValidator {
    private final HttpGatewayRemoteServicesIndex servicesIndex;
    private final HttpGatewayConfigAccessControl gatewayClients;

    public HttpGatewayClientValidator(HttpGatewayRemoteServicesIndex servicesIndex, HttpGatewayConfigAccessControl gatewayClients) {
        this.servicesIndex = servicesIndex;
        this.gatewayClients = gatewayClients;
    }

    /**
     * Identify a client.
     * @return The validated clientId, else an {@link HttpResponseStatus#UNAUTHORIZED} error
     */
    public HttpGatewayValidation<String> validateClientIdentification(Http.Request downstreamRequest) {
        return HttpGatewayClientValidators.validateClientIdentification(gatewayClients, downstreamRequest);
    }

    /**
     * Validate that a client has access to a route
     * @return The validated route with the associated service, else an {@link HttpResponseStatus#UNAUTHORIZED} error
     */
    public HttpGatewayValidation<HttpGatewayDestinationService> validateClientAccess(
        Http.Request downstreamRequest, DestinationRoute destinationRoute, String clientId
    ) {
        String serviceId = servicesIndex.findService(destinationRoute.getRouteId()).getServiceId();
        if (!gatewayClients.hasAccess(clientId, destinationRoute.getRouteId(), serviceId)) {
            return HttpGatewayValidation.ofError(
                HttpResponseStatus.UNAUTHORIZED,
                "Access denied to route " + downstreamRequest.method() + " " + downstreamRequest.path() + " for clientId " + clientId
            );
        }
        return HttpGatewayValidation.ofValue(new HttpGatewayDestinationService(destinationRoute, serviceId));
    }
}
