package com.coreoz.http;

import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.conf.HttpGatewayConfiguration;
import com.coreoz.http.conf.HttpGatewayRouterConfiguration;
import com.coreoz.http.config.HttpGatewayConfigClientAuth;
import com.coreoz.http.config.HttpGatewayConfigServices;
import com.coreoz.http.config.HttpGatewayConfigServicesAuth;
import com.coreoz.http.play.HttpGatewayDownstreamResponses;
import com.coreoz.http.router.HttpGatewayRouter;
import com.coreoz.http.router.data.DestinationRoute;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstream.HttpGatewayPeekingUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamKeepingResponse;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.coreoz.http.upstream.HttpGatewayUpstreamStringPeekerClient;
import com.coreoz.http.upstream.publisher.PeekerPublishersConsumer;
import com.coreoz.http.validation.HttpGatewayClientValidators;
import com.coreoz.http.validation.HttpGatewayRouteValidator;
import com.coreoz.http.validation.HttpGatewayValidation;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class SampleCustomRouting {
    static int HTTP_GATEWAY_PORT = 8080;

    public static void main(String[] args) {
        startsGateway();
    }

    public static HttpGateway startsGateway() {
        Config config = ConfigFactory.load("custom-routing.conf");
        List<? extends Config> clientsConfig = config.getConfigList("clients");
        Map<String, String> customerTypeByClientId = indexCustomerType(clientsConfig);
        HttpGatewayClientAuthenticator clientsAuthenticator = HttpGatewayConfigClientAuth.readAuth(clientsConfig);
        Map<String, RoutingPerCustomer> servicesIndexByCustomer = indexServicesByCustomer(config);
        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();

        return HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            routingDsl -> routingDsl.addRoutes(HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // validation
                HttpGatewayValidation<ValidationResult> validation = HttpGatewayClientValidators
                    .validateClientIdentification(clientsAuthenticator, downstreamRequest)
                    .then(clientId -> {
                        // use custom index to resolve routing
                        RoutingPerCustomer clientRoutingConfiguration = servicesIndexByCustomer.get(customerTypeByClientId.get(clientId));
                        return clientRoutingConfiguration
                            .getRouteValidator()
                            .validate(downstreamRequest)
                            .then(destinationRoute -> HttpGatewayValidation.ofValue(new ValidationResult(
                                destinationRoute,
                                clientRoutingConfiguration.getRemoteServicesAuthenticator(),
                                clientRoutingConfiguration.getServicesIndex()
                            )));
                    });
                // error management
                if (validation.isError()) {
                    logger.warn(validation.error().getMessage());
                    return HttpGatewayDownstreamResponses.buildError(validation.error());
                }
                DestinationRoute destinationRoute = validation.value().destinationRoute;
                HttpGatewayRemoteService destinationService = validation.value().getServicesIndex().findService(destinationRoute.getRouteId());

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationRoute.getDestinationUrl())
                    .with(validation.value().getRemoteServicesAuthenticator().forRoute(
                        destinationService.getServiceId(), destinationRoute.getRouteId()
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
            }))
        ));
    }

    private static Map<String, String> indexCustomerType(List<? extends Config> clientsConfig) {
        return clientsConfig
            .stream()
            .collect(Collectors.toMap(
                clientConfig -> clientConfig.getString("client-id"),
                clientConfig -> clientConfig.getString("customer-type")
            ));
    }

    @Value
    static class ValidationResult {
        DestinationRoute destinationRoute;
        HttpGatewayRemoteServicesAuthenticator remoteServicesAuthenticator;
        HttpGatewayRemoteServicesIndex servicesIndex;
    }

    @Value
    static class RoutingPerCustomer {
        HttpGatewayRemoteServicesIndex servicesIndex;
        HttpGatewayRouter httpRouter;
        HttpGatewayRouteValidator routeValidator;
        HttpGatewayRemoteServicesAuthenticator remoteServicesAuthenticator;
    }

    static Map<String, RoutingPerCustomer> indexServicesByCustomer(Config config) {
        Config baseCustomersRouting = config.getConfig("routing-per-customer");
        return baseCustomersRouting
            .root()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                customerEntry -> {
                    Config customerConfig = baseCustomersRouting.getConfig(customerEntry.getKey());
                    HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigServices.readConfig(customerConfig);
                    HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.computeValidatedIndexedRoutes());
                    HttpGatewayRemoteServicesAuthenticator remoteServicesAuthenticator = HttpGatewayConfigServicesAuth.readConfig(customerConfig);
                    return new RoutingPerCustomer(
                        servicesIndex,
                        httpRouter,
                        new HttpGatewayRouteValidator(httpRouter, servicesIndex),
                        remoteServicesAuthenticator
                    );
                }
            ));
    }
}
