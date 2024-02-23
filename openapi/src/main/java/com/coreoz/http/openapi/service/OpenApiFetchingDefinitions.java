package com.coreoz.http.openapi.service;

import com.coreoz.http.openapi.merge.OpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.upstream.HttpGatewayResponseStatus;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.apache.pekko.stream.javadsl.StreamConverters;
import org.reactivestreams.Publisher;
import play.mvc.Http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class OpenApiFetchingDefinitions implements OpenApiFetchingService {
    private final HttpGatewayUpstreamClient upstreamClient;
    private final HttpGatewayRemoteServicesIndex remoteServicesIndex;
    private final Map<String, OpenApiUpstreamParameters> openApiUpstreamParameters;

    public OpenApiFetchingDefinitions(
        HttpGatewayUpstreamClient upstreamClient,
        HttpGatewayRemoteServicesIndex remoteServicesIndex,
        List<OpenApiUpstreamParameters> openApiUpstreamParameters
    ) {
        this.upstreamClient = upstreamClient;
        this.remoteServicesIndex = remoteServicesIndex;
        this.openApiUpstreamParameters = openApiUpstreamParameters
            .stream()
            .collect(Collectors.toMap(
                OpenApiUpstreamParameters::serviceId,
                Function.identity()
            ));
    }

    @Override
    public CompletableFuture<OpenAPI> fetch() {
        Map<String, HttpGatewayRemoteService> indexedRemoteService = remoteServicesIndex
            .getServices()
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteService::getServiceId,
                Function.identity()
            ));
        @SuppressWarnings("unchecked")
        CompletableFuture<ServiceWithOpenApi>[] fetchingSpecifications = openApiUpstreamParameters
            .values()
            .stream()
            .map((OpenApiUpstreamParameters openApiParams) -> fetchRemoteOpenApi(
                openApiParams,
                indexedRemoteService.get(openApiParams.serviceId())
            ))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture
            .allOf(fetchingSpecifications)
            .thenApply(ignored -> Stream
                .of(fetchingSpecifications)
                .map(futureOpenApi -> {
                    try {
                        return futureOpenApi.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(
                    new OpenAPI(),
                    (OpenAPI consolidatedOpenApi, ServiceWithOpenApi serviceOpenApi) -> OpenApiMerger.addDefinitions(
                        consolidatedOpenApi,
                        serviceOpenApi.openApi(),
                        new OpenApiMergerConfiguration(
                            serviceOpenApi
                                .remoteService()
                                .getRoutes()
                                .stream()
                                .map(remoteServicesIndex::serviceRouteToHttpEndpoint)
                                .toList(),
                            capitalize(serviceOpenApi.remoteService().getServiceId()),
                            serviceOpenApi.remoteService().getServiceId(),
                            true
                        )
                    ),
                    // this is not made to be run in parallel
                    null
                )
            );
    }


    private CompletableFuture<ServiceWithOpenApi> fetchRemoteOpenApi(
        OpenApiUpstreamParameters openApiUpstreamParameters, HttpGatewayRemoteService remoteService
    ) {
        // TODO handle OpenAPI files that are put directly in the src/main/resources folder
        HttpGatewayUpstreamRequest request = upstreamClient
            .prepareRequest(new Http.RequestBuilder().build())
            .withUrl(remoteService.getBaseUrl() + openApiUpstreamParameters.openApiRemotePath())
            .with(openApiUpstreamParameters.upstreamAuthenticator());
        return upstreamClient
            .executeUpstreamRequest(request)
            .thenApply(response -> {
                // responseBodyString = new String(publisherToInputStream(response.getPublisher()).readAllBytes(), );
                if (response.getResponseStatus() != HttpGatewayResponseStatus.OK) {
                    logger.error("Could not fetch remote OpenAPI definition: {}", response);
                    return new ServiceWithOpenApi(remoteService, new OpenAPI());
                }
                // TODO finish implementing this
                return null;
            });
    }

    private static InputStream publisherToInputStream(Publisher<?> publisher) {
        // StreamConverters.asInputStream() ?
        if (publisher == null) {
            return InputStream.nullInputStream();
        }
        // TODO look if there isn't already a Publisher to InputStream interface
        return InputStream.nullInputStream();
    }

    private static record ServiceWithOpenApi(HttpGatewayRemoteService remoteService, OpenAPI openApi) { }

    private static String capitalize(String content) {
        if (content.isEmpty()) {
            return content;
        }
        return content.substring(0, 1).toUpperCase() + content.substring(1);
    }
}
