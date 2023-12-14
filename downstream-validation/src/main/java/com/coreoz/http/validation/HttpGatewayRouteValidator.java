package com.coreoz.http.validation;

import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.router.data.DestinationRoute;
import io.netty.handler.codec.http.HttpResponseStatus;
import play.mvc.Http;

/**
 * Validate that a route exists for an incoming downstream request
 */
public class HttpGatewayRouteValidator {
    private final HttpGatewayRouter httpRouter;
    private final HttpGatewayRemoteServicesIndex servicesIndex;

    public HttpGatewayRouteValidator(HttpGatewayRouter httpRouter, HttpGatewayRemoteServicesIndex servicesIndex) {
        this.httpRouter = httpRouter;
        this.servicesIndex = servicesIndex;
    }

    /**
     * Validate and identify the target service route
     * @return The validated target route, else an {@link HttpResponseStatus#NOT_FOUND} error
     */
    public HttpGatewayValidation<DestinationRoute> validate(Http.Request downstreamRequest) {
        return httpRouter
            .searchRoute(downstreamRequest.method(), downstreamRequest.path())
            .map(matchingRoute -> httpRouter.computeDestinationRoute(matchingRoute, servicesIndex.findServiceBaseUrl(matchingRoute)))
            .map(HttpGatewayValidation::ofValue)
            .orElseGet(() -> HttpGatewayValidation.ofError(
                HttpResponseStatus.NOT_FOUND,
                "No route exists for " + downstreamRequest.method() + " " + downstreamRequest.path()
            ));
    }
}
