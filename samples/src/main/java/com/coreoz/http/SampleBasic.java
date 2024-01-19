package com.coreoz.http;

import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.HttpGatewayConfigClientAccessControl;
import com.coreoz.http.config.HttpGatewayConfigLoader;
import com.coreoz.http.config.HttpGatewayConfigServices;
import com.coreoz.http.config.HttpGatewayConfigServicesAuth;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
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

/**
 * This sample feature the standard basic Gateway with:
 * - Clients which are authenticated
 * - Clients which have only a few authorized services/routes access
 * - Services and route definition with service authentication
 */
@Slf4j
public class SampleBasic {
    static int HTTP_GATEWAY_PORT = 8080;

    public static void main(String[] args) {
        startsGateway();
    }

    public static HttpGateway startsGateway() {
        HttpGatewayConfigLoader configLoader = new HttpGatewayConfigLoader();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigServices.readConfig(configLoader);
        HttpGatewayRemoteServicesAuthenticator remoteServicesAuthenticator = HttpGatewayConfigServicesAuth.readConfig(configLoader);
        HttpGatewayConfigClientAccessControl gatewayClients = HttpGatewayConfigClientAccessControl.readConfig(configLoader).validateConfig(servicesIndex);
        HttpGatewayClientValidator clientValidator = new HttpGatewayClientValidator(servicesIndex, gatewayClients);
        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.computeValidatedIndexedRoutes());
        HttpGatewayRouteValidator routeValidator = new HttpGatewayRouteValidator(httpRouter, servicesIndex);

        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();

        return HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // validation
                HttpGatewayValidation<HttpGatewayDestinationService> validation = clientValidator
                    .validateClientIdentification(downstreamRequest)
                    .then(clientId -> routeValidator
                        .validate(downstreamRequest)
                        .then(destinationRoute -> clientValidator.validateClientAccess(downstreamRequest, destinationRoute, clientId))
                    );
                // error management
                if (validation.isError()) {
                    logger.warn(validation.error().getMessage());
                    return HttpGatewayDownstreamResponses.buildError(validation.error());
                }
                HttpGatewayDestinationService destinationService = validation.value();

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationService.getDestinationRoute().getDestinationUrl())
                    .with(remoteServicesAuthenticator.forRoute(
                        destinationService.getServiceId(), destinationService.getDestinationRoute().getRouteId()
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
                        // Set an empty body response
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
