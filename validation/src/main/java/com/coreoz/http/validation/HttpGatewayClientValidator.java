package com.coreoz.http.validation;

import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import com.coreoz.http.router.data.DestinationRoute;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.mvc.Http;

/**
 * Validate authorized clients from incoming downstream requests
 */
public class HttpGatewayClientValidator {
    private final HttpGatewayRouteValidator routeValidator;
    private final HttpGatewayConfigAccessControl gatewayClients;

    public HttpGatewayClientValidator(HttpGatewayRouteValidator routeValidator, HttpGatewayConfigAccessControl gatewayClients) {
        this.routeValidator = routeValidator;
        this.gatewayClients = gatewayClients;
    }

    /**
     * Identify a client.
     * @return The validated clientId, else an {@link HttpResponseStatus#UNAUTHORIZED} error
     */
    public HttpGatewayValidation<String> validateClientIdentification(Http.Request downstreamRequest) {
        String clientId = gatewayClients.authenticate(downstreamRequest);
        if (clientId == null) {
            return HttpGatewayValidation.ofError(HttpResponseStatus.UNAUTHORIZED, "Client authentication failed");
        }
        return HttpGatewayValidation.ofValue(clientId);
    }

    public HttpGatewayValidation<HttpGatewayDestinationService> validateClientAccess(
        Http.Request downstreamRequest, DestinationRoute destinationRoute, String clientId
    ) {
        String serviceId = routeValidator.getServicesIndex().findService(destinationRoute.getRouteId()).getServiceId();
        if (!gatewayClients.hasAccess(clientId, destinationRoute.getRouteId(), serviceId)) {
            return HttpGatewayValidation.ofError(
                HttpResponseStatus.UNAUTHORIZED,
                "Access denied to route " + downstreamRequest.method() + " " + downstreamRequest.path() + " for clientId " + clientId
            );
        }
        return HttpGatewayValidation.ofValue(new HttpGatewayDestinationService(destinationRoute, serviceId));
    }
}
