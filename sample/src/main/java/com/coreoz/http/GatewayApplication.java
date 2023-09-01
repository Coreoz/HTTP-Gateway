package com.coreoz.http;

import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import com.coreoz.http.config.HttpGatewayConfigLoader;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.config.HttpGatewayConfigRemoteServices;
import com.coreoz.http.router.data.DestinationRoute;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.CompletableFuture;

public class GatewayApplication {
    static int HTTP_GATEWAY_PORT = 8080;

    public static void main(String[] args) {
        HttpGatewayConfigLoader configLoader = new HttpGatewayConfigLoader();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigRemoteServices.readConfig(configLoader);
        HttpGatewayConfigAccessControl gatewayClients = HttpGatewayConfigAccessControl.readConfig(configLoader);

        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.getRoutes());

        HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();

        HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // TODO how errors can be handled in a clean way?
                // TODO make a class that gather clientAuthentication and routing resolution
                // TODO add logs when error occurs
                String clientId = gatewayClients.authenticate(downstreamRequest);
                if (clientId == null) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.UNAUTHORIZED, "Client authentication failed");
                }

                DestinationRoute destinationRoute = httpRouter
                    .searchRoute(downstreamRequest.method(), downstreamRequest.path())
                    .map((matchingRoute) -> httpRouter.computeDestinationRoute(matchingRoute, servicesIndex.findServiceBaseUrl(matchingRoute)))
                    .orElse(null);
                if (destinationRoute == null) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                // TODO Could we make this code easier ?
                if (!gatewayClients.hasAccess(clientId, destinationRoute.getRouteId(), servicesIndex.findService(destinationRoute.getRouteId()).getServiceId())) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.UNAUTHORIZED, "Access denied to route " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                // TODO ajouter du publisher peeker via la méthode preparePeekerReques
                HttpGatewayUpstreamRequest remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationRoute.getDestinationUrl())
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayUpstreamResponse> upstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);
                return upstreamFutureResponse.thenApply(upstreamResponse -> {
                    if (upstreamResponse.getStatusCode() > HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        upstreamResponse.setPublisher(null);
                    }

                    return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
                });
            })
        ));
    }
}
