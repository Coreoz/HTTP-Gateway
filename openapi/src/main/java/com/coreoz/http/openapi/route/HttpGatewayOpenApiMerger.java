package com.coreoz.http.openapi.route;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.openapi.merge.OpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.google.common.base.Predicates;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class HttpGatewayOpenApiMerger {
    public static @NotNull CompletionStage<OpenAPI> fetchToUnifiedOpenApi(@NotNull OpenApiRouteConfiguration config) {
        Map<String, HttpGatewayRemoteService> indexedRemoteService = config
            .remoteServicesIndex()
            .getServices()
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteService::getServiceId,
                Function.identity()
            ));
        @SuppressWarnings("unchecked")
        CompletableFuture<OpenApiFetchingData>[] fetchingSpecifications = config
            .openApiFetchers()
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
                        logger.error("Failed to load openApi definition", e);
                        return null;
                    }
                })
                .filter(Predicates.notNull())
                .reduce(
                    config.baseOpenApi(),
                    (OpenAPI consolidatedOpenApi, OpenApiFetchingData serviceOpenApi) -> OpenApiMerger.addDefinitions(
                        consolidatedOpenApi,
                        serviceOpenApi.openApiDefinition(),
                        new OpenApiMergerConfiguration(
                            indexedRemoteService
                                // TODO handle error if service is not found in indexedRemoteService
                                .get(serviceOpenApi.serviceId())
                                .getRoutes()
                                .stream()
                                .map(config.remoteServicesIndex()::serviceRouteToHttpEndpoint)
                                .toList(),
                            config.componentNamePrefixMaker().apply(serviceOpenApi.serviceId()),
                            config.operationIdPrefixMaker().apply(serviceOpenApi.serviceId()),
                            config.createMissingEndpoints()
                        )
                    ),
                    (a, b) -> {
                        throw new RuntimeException("Not supported");
                    }
                )
            );
    }
}
