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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Http;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class GatewayApplication {
    static int HTTP_GATEWAY_PORT = 8080;

    // TODO make a basic sample without customization
    public static void main(String[] args) {
        startsGateway();
    }

    public static HttpGateway startsGateway() {
        HttpGatewayConfigLoader configLoader = new HttpGatewayConfigLoader();
        HttpGatewayRemoteServicesIndex servicesIndex = HttpGatewayConfigRemoteServices.readConfig(configLoader);
        HttpGatewayRemoteServiceAuthenticator remoteServiceAuthenticator = HttpGatewayConfigRemoteServicesAuth.readConfig(configLoader);
        HttpGatewayConfigAccessControl gatewayClients = HttpGatewayConfigAccessControl.readConfig(configLoader).validateConfig(servicesIndex);
        HttpGatewayClientValidator clientValidator = new HttpGatewayClientValidator(servicesIndex, gatewayClients);
        HttpGatewayRouter httpRouter = new HttpGatewayRouter(servicesIndex.computeValidatedIndexedRoutes());
        HttpGatewayRouteValidator routeValidator = new HttpGatewayRouteValidator(httpRouter, servicesIndex);
        // custom validation
        Map<String, CustomClientAttributes> clientsCustomAttributes = readClientsCustomAttributes(configLoader);

        // HttpGatewayUpstreamClient httpGatewayUpstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamStringPeekerClient httpGatewayUpstreamClient = new HttpGatewayUpstreamStringPeekerClient();

        return HttpGateway.start(new HttpGatewayConfiguration(
            HTTP_GATEWAY_PORT,
            HttpGatewayRouterConfiguration.asyncRouting(downstreamRequest -> {
                // validation
                HttpGatewayValidation<CustomClientValidation> validation = clientValidator
                    .validateClientIdentification(downstreamRequest)
                    .then(clientId -> routeValidator
                        .validate(downstreamRequest)
                        .then(destinationRoute -> clientValidator.validateClientAccess(downstreamRequest, destinationRoute, clientId))
                        // custom validation
                        .then(destinationService -> validateCustomClientAuthorization(
                            downstreamRequest, clientsCustomAttributes.get(clientId), destinationService
                        ))
                    );
                // error management
                if (validation.isError()) {
                    logger.warn(validation.error().getMessage());
                    return HttpGatewayDownstreamResponses.buildError(validation.error());
                }
                HttpGatewayDestinationService destinationService = validation.value().getDestinationService();

                HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest = httpGatewayUpstreamClient
                    .prepareRequest(downstreamRequest)
                    .withUrl(destinationService.getDestinationRoute().getDestinationUrl())
                    .with(remoteServiceAuthenticator.forRoute(
                        destinationService.getServiceId(), destinationService.getDestinationRoute().getRouteId()
                    ))
                    // forward custom header value
                    .with((unused, upstreamRequest) -> {
                        upstreamRequest.setHeader(HTTP_HEADER_TENANTS, validation.value().customClientAttributes.getTenants());
                    })
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

    // Custom authorization

    static String HTTP_HEADER_TENANTS = "X-Tenants";

    @Value
    static class CustomClientAttributes {
        Set<String> tenants;
    }

    @Value
    static class CustomClientValidation {
        HttpGatewayDestinationService destinationService;
        CustomClientAttributes customClientAttributes;
    }

    private static Map<String, CustomClientAttributes> readClientsCustomAttributes(HttpGatewayConfigLoader configLoader) {
        return configLoader
            .getHttpGatewayConfig()
            .getConfigList("clients")
            .stream()
            .collect(Collectors.toMap(
                clientConfig -> clientConfig.getString("client-id"),
                clientConfig -> new CustomClientAttributes(
                    new HashSet<>(clientConfig.getStringList("tenants"))
                )
            ));
    }

    private static HttpGatewayValidation<CustomClientValidation> validateCustomClientAuthorization(
        Http.Request downstreamRequest,
        CustomClientAttributes clientCustomAttributes,
        HttpGatewayDestinationService destinationService
        ) {
        // Fetch custom downstream header values
        String[] requestedTenants = downstreamRequest.header(HTTP_HEADER_TENANTS).map(tenants -> tenants.split(",")).orElse(null);
        if (requestedTenants == null) {
            return HttpGatewayValidation.ofError(HttpResponseStatus.PRECONDITION_FAILED, "Missing header " + HTTP_HEADER_TENANTS);
        }

        // Verify client access to the requested values
        for (String requestedTenant : requestedTenants) {
            if (!clientCustomAttributes.getTenants().contains(requestedTenant)) {
                return HttpGatewayValidation.ofError(HttpResponseStatus.UNAUTHORIZED, "Access to tenant '" + requestedTenant + "' is not allowed. Allowed tenants are: " + clientCustomAttributes.getTenants());
            }
        }

        // return success validation with the custom value
        return HttpGatewayValidation.ofValue(new CustomClientValidation(
            destinationService,
            new CustomClientAttributes(Set.of(requestedTenants))
        ));
    }
}
