package com.coreoz.http;

import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.*;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcherConfiguration;
import com.coreoz.http.openapi.route.OpenApiRoute;
import com.coreoz.http.openapi.route.OpenApiRouteConfiguration;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.publisher.PeekerPublishersConsumer;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstream.*;
import com.coreoz.http.validation.HttpGatewayClientValidator;
import com.coreoz.http.validation.HttpGatewayDestinationService;
import com.coreoz.http.validation.HttpGatewayRouteValidator;
import com.coreoz.http.validation.HttpGatewayValidation;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        HttpGatewayConfigClientAccessControl gatewayClients = HttpGatewayConfigClientAccessControl
            .readConfig(configLoader, List.of(HttpGatewayConfigClientAuth.KEY_AUTH))
            .validateConfig(servicesIndex);
        HttpGatewayClientValidator clientValidator = new HttpGatewayClientValidator(servicesIndex, gatewayClients);
        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.computeValidatedIndexedRoutes());
        HttpGatewayRouteValidator routeValidator = new HttpGatewayRouteValidator(httpRouter, servicesIndex);

        HttpGatewayUpstreamClient upstreamBaseClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient(
            HttpGatewayStringStreamPeekingConfiguration.DEFAULT_CONFIG, upstreamBaseClient
        );

        // OpenApi configuration
        // TODO update the configuration service to create the correct fetcher per service id
        Map<String, HttpGatewayRemoteService> servicesIndexedById = servicesIndex
            .getServices()
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteService::getServiceId,
                Function.identity()
            ));
        OpenApiRoute openApiRoute = new OpenApiRoute(new OpenApiRouteConfiguration(
            HttpGatewayConfigOpenApiServices
                .readConfig(configLoader)
                .stream()
                .map(serviceOpenApiConfig -> {
                    // TODO to be created in the openapi configuration service
                    return new OpenApiHttpFetcher(new OpenApiHttpFetcherConfiguration(
                        serviceOpenApiConfig.serviceId(),
                        servicesIndexedById.get(serviceOpenApiConfig.serviceId()).getBaseUrl() + serviceOpenApiConfig.openApiRemotePath(),
                        upstreamBaseClient,
                        serviceOpenApiConfig.upstreamAuthenticator()
                    ));
                })
                .toList(),
            servicesIndex
        ));

        return HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routerDsl -> routerDsl
                // Additional routing must be set before the main HTTP Gateway routing part
                .addRoutes(openApiRoute)
                .addRoutes(HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                    // starts validation
                    HttpGatewayValidation<String> clientValidation = clientValidator.validateClientIdentification(downstreamRequest);

                    // validation
                    HttpGatewayValidation<HttpGatewayDestinationService> validation = clientValidation
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
                }))
        ));
    }
}
