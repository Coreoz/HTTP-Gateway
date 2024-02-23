package com.coreoz.http.openapi.service;

import com.coreoz.http.openapi.merge.OpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import io.swagger.v3.oas.models.OpenAPI;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OpenApiFetchingDefinitions implements OpenApiFetchingService {
    private final HttpGatewayUpstreamClient upstreamClient;
    private final HttpGatewayRemoteServicesIndex remoteServicesIndex;
    private final List<OpenApiUpstreamParameters> openApiUpstreamParameters;

    // TODO cache

    public OpenApiFetchingDefinitions(
        HttpGatewayUpstreamClient upstreamClient,
        HttpGatewayRemoteServicesIndex remoteServicesIndex,
        List<OpenApiUpstreamParameters> openApiUpstreamParameters) {
        this.upstreamClient = upstreamClient;
        this.remoteServicesIndex = remoteServicesIndex;
        this.openApiUpstreamParameters = openApiUpstreamParameters;
    }

    // TODO cache initialization method with
    //  - fetching all remote OpenAPI spec
    //  - creating a new OpenAPi from all other using the routes referenced in the HTTP API Gateway service config
    //  - filter available routes

    @Override
    public CompletableFuture<OpenAPI> fetch() {
        @SuppressWarnings("unchecked")
        CompletableFuture<OpenAPI>[] fetchingSpecifications = openApiUpstreamParameters
            .stream()
            .map(this::fetchRemoteOpenApi)
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
                    (OpenAPI consolidatedOpenApi, OpenAPI serviceOpenApi) -> OpenApiMerger.addDefinitions(
                        consolidatedOpenApi,
                        serviceOpenApi,
                        // TODO new OpenApiMergerConfiguration
                        null
                    )));
    }


    private CompletableFuture<OpenAPI> fetchRemoteOpenApi(OpenApiUpstreamParameters openApiUpstreamParameters) {
        // upstreamClient.executeUpstreamRequest();
        // TODO to implements
        return CompletableFuture.completedFuture(null);
    }
}
