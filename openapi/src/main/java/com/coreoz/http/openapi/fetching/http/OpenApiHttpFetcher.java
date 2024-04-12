package com.coreoz.http.openapi.fetching.http;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchers;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.publisher.ByteReaders;
import com.coreoz.http.publisher.BytesReaderPublishers;
import com.coreoz.http.publisher.HttpCharsetParser;
import com.coreoz.http.upstream.HttpGatewayResponseStatus;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.mvc.Http;

import java.util.concurrent.CompletableFuture;

/**
 * An {@link OpenApiFetcher} that will fetch an OpenAPI file from a remote HTTP endpoint.
 * This HTTP endpoint can optionally require an authentication.
 */
@Slf4j
public class OpenApiHttpFetcher implements OpenApiFetcher {
    private final OpenApiHttpFetcherConfiguration configuration;

    public OpenApiHttpFetcher(OpenApiHttpFetcherConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public @NotNull CompletableFuture<OpenApiFetchingData> fetch() {
        HttpGatewayUpstreamRequest request = configuration
            .upstreamClient()
            .prepareRequest(new Http.RequestBuilder().build())
            .withUrl(configuration.remoteUrl());
        if (configuration.upstreamAuthenticator() != null) {
            request = request.with(configuration.upstreamAuthenticator());
        }
        return configuration
            .upstreamClient()
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
                        "Could not fetch remote OpenAPI definition for service {} on URL {} : responseStatut={} - exception={} - status code={} - response body={}",
                        configuration.serviceId(),
                        configuration.remoteUrl(),
                        response.response().getResponseStatus(),
                        response.response().getGatewayError(),
                        response.response().getStatusCode(),
                        responseBodyString
                    );
                    return null;
                }
                if (responseBodyString == null) {
                    logger.warn("Fetched empty OpenApi Definition for service {} on URL {}", configuration.serviceId(), configuration.remoteUrl());
                    return null;
                }

                OpenAPI openApiContent = OpenApiFetchers.parseOpenApiContent(responseBodyString, configuration.serviceId());
                return openApiContent == null ?
                    null
                    : new OpenApiFetchingData(
                        configuration.serviceId(),
                        openApiContent
                    );
            })
            .exceptionally(error -> {
                logger.error(
                    "Failed to fetch openAPI definitions for service {} and URL {}",
                    configuration.serviceId(),
                    configuration.remoteUrl(),
                    error
                );
                return null;
            });
    }

    private record CompleteResponse(HttpGatewayUpstreamResponse response, byte[] responseBody) { }
}
