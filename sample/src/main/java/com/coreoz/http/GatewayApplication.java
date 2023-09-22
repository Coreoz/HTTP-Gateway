package com.coreoz.http;

import com.coreoz.http.upstream.*;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import com.coreoz.http.config.HttpGatewayConfigLoader;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.config.HttpGatewayConfigRemoteServices;
import com.coreoz.http.config.HttpGatewayConfigRemoteServicesAuth;
import com.coreoz.http.router.data.DestinationRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuthenticator;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class GatewayApplication {
    static int HTTP_GATEWAY_PORT = 8080;

    public static void main(String[] args) {
        HttpGatewayConfigLoader configLoader = new HttpGatewayConfigLoader();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigRemoteServices.readConfig(configLoader);
        HttpGatewayRemoteServiceAuthenticator remoteServiceAuthenticator = HttpGatewayConfigRemoteServicesAuth.readConfig(configLoader);
        HttpGatewayConfigAccessControl gatewayClients = HttpGatewayConfigAccessControl.readConfig(configLoader);

        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.getRoutes());

        // HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();

        HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // TODO how errors can be handled in a clean way?
                //      => Create a HttpGatewayError type
                //      => mate it easy to log and return it
                //      => provide a way to monad it: firstMethod.ifSuccess((params) -> secondMethod(params))
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

                String remoteServiceId = servicesIndex.findService(destinationRoute.getRouteId()).getServiceId();
                if (!gatewayClients.hasAccess(clientId, destinationRoute.getRouteId(), remoteServiceId)) {
                    return HttpGatewayDownstreamResponses.buildError(HttpResponseStatus.UNAUTHORIZED, "Access denied to route " + downstreamRequest.method() + " " + downstreamRequest.path());
                }

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationRoute.getDestinationUrl())
                    .with(remoteServiceAuthenticator.forRoute(remoteServiceId, destinationRoute.getRouteId()))
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayUpstreamKeepingResponse<String, String>> peekingUpstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);
                return peekingUpstreamFutureResponse.thenApply(peekingUpstreamResponse -> {
                    HttpGatewayUpstreamResponse upstreamResponse = peekingUpstreamResponse.getUpstreamResponse();
                    if (upstreamResponse.getStatusCode() > HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        upstreamResponse.setPublisher(null);
                    }

                    peekingUpstreamResponse.getStreamsPeeked().thenAccept(peekedStreams -> {
                       logger.debug("Proxied request: downstream={} upstream={}", peekedStreams.getDownstreamPeeking(), peekedStreams.getUpstreamPeeking());
                    });

                    return HttpGatewayDownstreamResponses.buildResult(upstreamResponse);
                });
            })
        ));
    }
}
