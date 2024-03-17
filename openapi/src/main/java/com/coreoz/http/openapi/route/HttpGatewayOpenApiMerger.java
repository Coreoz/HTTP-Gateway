package com.coreoz.http.openapi.route;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.openapi.merge.OpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.services.HttpGatewayRemoteService;
import io.swagger.v3.oas.models.OpenAPI;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpGatewayOpenApiMerger {
    public static @NotNull CompletionStage<OpenAPI> fetchToUnifiedOpenApi(@NotNull OpenApiRouteConfiguration config) {
        Map<String, HttpGatewayRemoteService> indexedRemoteService = config
            .getRemoteServicesIndex()
            .getServices()
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteService::getServiceId,
                Function.identity()
            ));
        @SuppressWarnings("unchecked")
        CompletableFuture<OpenApiFetchingData>[] fetchingSpecifications = config
            .getOpenApiFetchers()
            .stream()
            .map(OpenApiFetcher::fetch)
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
                    config.getBaseOpenApi(),
                    (OpenAPI consolidatedOpenApi, OpenApiFetchingData serviceOpenApi) -> OpenApiMerger.addDefinitions(
                        consolidatedOpenApi,
                        serviceOpenApi.openApiDefinition(),
                        new OpenApiMergerConfiguration(
                            indexedRemoteService
                                // TODO handle error if service is not found in indexedRemoteService
                                .get(serviceOpenApi.serviceId())
                                .getRoutes()
                                .stream()
                                .map(config.getRemoteServicesIndex()::serviceRouteToHttpEndpoint)
                                .toList(),
                            config.getComponentNamePrefixMaker().apply(serviceOpenApi.serviceId()),
                            config.getOperationIdPrefixMaker().apply(serviceOpenApi.serviceId()),
                            config.isCreateMissingEndpoints()
                        )
                    ),
                    (a, b) -> {
                        throw new RuntimeException("Not supported");
                    }
                )
            );
    }
}
