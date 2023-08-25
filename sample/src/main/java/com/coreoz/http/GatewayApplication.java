package com.coreoz.http;

import com.coreoz.http.client.HttpGatewayUpstreamClient;
import com.coreoz.http.client.HttpGatewayUpstreamRequest;
import com.coreoz.http.client.HttpGatewayUpstreamResponse;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.remote.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.router.config.HttpGatewayConfigRemoteServices;
import com.coreoz.http.router.data.DestinationRoute;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.concurrent.CompletableFuture;

public class GatewayApplication {
    static int HTTP_GATEWAY_PORT = 8080;

    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigRemoteServices.indexRemoteServices(config);
        // TODO load the clients
        gatewayClients = HttpGatewayConfigClientApiKey.index(config);

        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.getRoutes());

        HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();

        HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // TODO client auth

                DestinationRoute destinationRoute = httpRouter
                    .searchRoute(downstreamRequest.method(), downstreamRequest.path())
                    .map((matchingRoute) -> httpRouter.computeDestinationRoute(matchingRoute, servicesIndex.findServiceBaseUrl(matchingRoute)))
                    .orElse(null);
                if (destinationRoute == null) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.NOT_FOUND, "No route exists for " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                // TODO client route access validation
                // destinationRoute.getRouteId()

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
