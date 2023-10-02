package com.coreoz.http;

import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.HttpGatewayConfigAccessControl;
import com.coreoz.http.config.HttpGatewayConfigLoader;
import com.coreoz.http.config.HttpGatewayConfigRemoteServices;
import com.coreoz.http.config.HttpGatewayConfigRemoteServicesAuth;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuthenticator;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.upstream.HttpGatewayPeekingUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamKeepingResponse;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.coreoz.http.upstream.HttpGatewayUpstreamStringPeekerClient;
import com.coreoz.http.upstream.publisher.PeekerPublishersConsumer;
import com.coreoz.http.validation.HttpGatewayClientValidator;
import com.coreoz.http.validation.HttpGatewayDestinationService;
import com.coreoz.http.validation.HttpGatewayRouteValidator;
import com.coreoz.http.validation.HttpGatewayValidation;
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
        HttpGatewayRouteValidator routeValidator = new HttpGatewayRouteValidator(httpRouter, servicesIndex);
        HttpGatewayClientValidator clientValidator = new HttpGatewayClientValidator(routeValidator, gatewayClients);

        // HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();

        HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // validation
                HttpGatewayValidation<HttpGatewayDestinationService> destinationServiceValidation = clientValidator
                    .validateClientIdentification(downstreamRequest)
                    .then(clientId -> routeValidator
                        .validate(downstreamRequest)
                        .then(destinationRoute -> clientValidator.validateClientAccess(downstreamRequest, destinationRoute, clientId))
                    );
                // error management
                if (destinationServiceValidation.isError()) {
                    logger.warn(destinationServiceValidation.error().getMessage());
                    return HttpGatewayDownstreamResponses.buildError(destinationServiceValidation.error());
                }
                HttpGatewayDestinationService destinationService = destinationServiceValidation.value();

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationService.getDestinationRoute().getDestinationUrl())
                    .with(remoteServiceAuthenticator.forRoute(
                        destinationService.getRemoteServiceId(), destinationService.getDestinationRoute().getRouteId()
                    ))
                    .copyBasicHeaders()
                    .copyQueryParams();
                CompletableFuture<HttpGatewayUpstreamKeepingResponse<String, String>> peekingUpstreamFutureResponse = httpGatewayUpstreamClient.executeUpstreamRequest(remoteRequest);
                return peekingUpstreamFutureResponse.thenApply(peekingUpstreamResponse -> {
                    HttpGatewayUpstreamResponse upstreamResponse = peekingUpstreamResponse.getUpstreamResponse();
                    if (upstreamResponse.getStatusCode() >= HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) {
                        // Do not forward the response body if the upstream server returns an internal error
                        // => this enables to avoid forwarding sensitive information
                        PeekerPublishersConsumer.consume(upstreamResponse.getPublisher());
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
