package com.coreoz.http.openapi.service;

import com.coreoz.http.openapi.merge.OpenApiMerger;
import com.coreoz.http.openapi.merge.OpenApiMergerConfiguration;
import com.coreoz.http.publisher.BytesReaderPublishers;
import com.coreoz.http.services.HttpGatewayRemoteService;
import com.coreoz.http.services.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.upstream.HttpGatewayResponseStatus;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.coreoz.http.publisher.ByteReaders;
import com.coreoz.http.publisher.HttpCharsetParser;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Http;

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
                    // TODO enable to customize base OpenAPI
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
                    (a, b) -> {
                        throw new RuntimeException("Not supported");
                    }
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
            .thenCompose(response -> {
                if (response.getPublisher() == null) {
                    return CompletableFuture.completedFuture(new CompleteResponse(response, null));
                }
                return BytesReaderPublishers
                    .publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart)
                    .thenApply(responseBody -> new CompleteResponse(response, responseBody));
            })
            .thenApply(response -> {
                String responseBodyString = response.responseBody() == null ? null : new String(
                    response.responseBody(),
                    HttpCharsetParser.parseEncodingFromHttpContentType(
                        response.response().getContentType()
                    )
                );
                if (response.response().getResponseStatus() != HttpGatewayResponseStatus.OK) {
                    logger.error(
                        "Could not fetch remote OpenAPI definition: responseStatut={} - exception={} - status code={} - response body={}",
                        response.response().getResponseStatus(),
                        response.response().getGatewayError(),
                        response.response().getStatusCode(),
                        responseBodyString
                    );
                    return new ServiceWithOpenApi(remoteService, new OpenAPI());
                }

                SwaggerParseResult openApiParsingResult = new OpenAPIParser().readContents(responseBodyString, null, null);
                return new ServiceWithOpenApi(remoteService, openApiParsingResult.getOpenAPI());
            })
            .exceptionally(error -> {
                logger.info("Failed to fetch openAPI definitions for service {}", remoteService, error);
                return new ServiceWithOpenApi(remoteService, new OpenAPI());
            });
    }

    private record ServiceWithOpenApi(HttpGatewayRemoteService remoteService, OpenAPI openApi) { }

    private record CompleteResponse(HttpGatewayUpstreamResponse response, byte[] responseBody) { }

    private static String capitalize(String content) {
        if (content.isEmpty()) {
            return content;
        }
        return content.substring(0, 1).toUpperCase() + content.substring(1);
    }
}
