package com.coreoz.http.openapi.route;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.openapi.merge.HttpGatewayOpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
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

/**
 * The OpenAPI files fetching and merging orchestrator
 */
// TODO to unit test
@Slf4j
public class OpenApiOrchestrator {
    /**
     * Perform 2 main actions:<br>
     * 1. Fetch in parallel all OpenAPI files<br>
     * 2. Merge all the OpenAPI files in a single OpenAPI definition<br>
     * @param config The OpenAPI route definition for which this function uses all the parameters except for the routePath
     * @return A {@link CompletableFuture} that resolves with the unified OpenAPI definition
     * TODO see also merge lib
     */
    public static @NotNull CompletionStage<OpenAPI> computeUnifiedOpenApi(@NotNull OpenApiRouteConfiguration config) {
        Map<String, HttpGatewayRemoteService> indexedRemoteService = indexRemoteServices(config.remoteServicesIndex());
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
                    (OpenAPI consolidatedOpenApi, OpenApiFetchingData serviceOpenApi) ->
                        mergeOpenApiSpecs(config, consolidatedOpenApi, serviceOpenApi, indexedRemoteService),
                    (a, b) -> {
                        throw new RuntimeException("Parallel stream reducing is not supported");
                    }
                )
            );
    }

    @NotNull
    private static Map<String, HttpGatewayRemoteService> indexRemoteServices(@NotNull HttpGatewayRemoteServicesIndex remoteServices) {
        return remoteServices
            .getServices()
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteService::serviceId,
                Function.identity()
            ));
    }

    @NotNull
    private static OpenAPI mergeOpenApiSpecs(@NotNull OpenApiRouteConfiguration config, OpenAPI consolidatedOpenApi, OpenApiFetchingData serviceOpenApi, Map<String, HttpGatewayRemoteService> indexedRemoteService) {
        return HttpGatewayOpenApiMerger.merge(
            consolidatedOpenApi,
            serviceOpenApi.openApiDefinition(),
            new OpenApiMergerConfiguration(
                indexedRemoteService
                    // TODO handle error if service is not found in indexedRemoteService
                    .get(serviceOpenApi.serviceId())
                    .routes()
                    .stream()
                    .map(config.remoteServicesIndex()::serviceRouteToHttpRoute)
                    .toList(),
                config.componentNamePrefixMaker().apply(serviceOpenApi.serviceId()),
                config.operationIdPrefixMaker().apply(serviceOpenApi.serviceId()),
                config.createMissingEndpoints()
            )
        );
    }
}
