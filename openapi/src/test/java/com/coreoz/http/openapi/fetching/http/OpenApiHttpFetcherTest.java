package com.coreoz.http.openapi.fetching.http;

import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.openapi.fetching.OpenApiTestFetchers;
import com.coreoz.http.publisher.StaticBytesPublisher;
import com.coreoz.http.upstream.HttpGatewayResponseStatus;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.HttpResponseBodyPart;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class OpenApiHttpFetcherTest {
    @Test
    public void verify_that_correct_fetching_of_openapi_spec_returns_openapi_document() throws ExecutionException, InterruptedException {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(makePetstorePublisher());
        Assertions.assertThat(mergedOpenApi).isNotNull();
        Assertions.assertThat(mergedOpenApi.serviceId()).isEqualTo("service-test");
        Assertions.assertThat(mergedOpenApi.openApiDefinition()).isNotNull();
        OpenApiTestFetchers.assertOpenApiTitle(mergedOpenApi, "Swagger Petstore");
    }

    @Test
    public void verify_that_null_authenticator_does_not_add_a_authorization_header() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(
            request -> makeStringPublisher(makeValidOpenApi("authorization="+request.getDownstreamRequest().header(HttpHeaders.AUTHORIZATION).orElse(null))),
            null
        );
        OpenApiTestFetchers.assertOpenApiTitle(mergedOpenApi, "authorization=null");
    }

    @Test
    public void verify_that_non_null_authenticator_adds_a_authorization_header() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(
            request -> makeStringPublisher(makeValidOpenApi("authorization="+request.getUpstreamRequest().build().getHeaders().get(HttpHeaders.AUTHORIZATION))),
            (downstreamRequest, upstreamRequest) -> upstreamRequest.setHeader(HttpHeaders.AUTHORIZATION, "custom-auth")
        );
        OpenApiTestFetchers.assertOpenApiTitle(mergedOpenApi, "authorization=custom-auth");
    }

    @Test
    public void verify_that_null_response_body_returns_null_data() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(null);
        Assertions.assertThat(mergedOpenApi).isNull();
    }

    @Test
    public void verify_that_encoding_is_correctly_interpreted() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(makeStringPublisher(makeValidOpenApi("éùàç")));
        OpenApiTestFetchers.assertOpenApiTitle(mergedOpenApi, "éùàç");
    }

    @Test
    public void verify_that_http_error_response_with_body_returns_null_data() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcherRaw(
            request -> {
                HttpGatewayUpstreamResponse response = makeBaseResponse();
                response.setResponseStatus(HttpGatewayResponseStatus.REMOTE_SERVER_ERROR);
                response.setPublisher(makeStringPublisher(makeValidOpenApi("test")));
                return CompletableFuture.completedFuture(response);
            },
            null
        );
        Assertions.assertThat(mergedOpenApi).isNull();
    }

    @Test
    public void verify_that_processing_exception_returns_null_data() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcherRaw(
            request -> {
                return CompletableFuture.failedFuture(new RuntimeException("PRocessing error"));
            },
            null
        );
        Assertions.assertThat(mergedOpenApi).isNull();
    }

    @Test
    public void verify_that_wrong_openapi_format_returns_null_data() {
        OpenApiFetchingData mergedOpenApi = executeHttpFetcher(makeStringPublisher("Not a valid openapi format"));
        Assertions.assertThat(mergedOpenApi).isNull();
    }

    // utils

    private OpenApiFetchingData executeHttpFetcher(Publisher<HttpResponseBodyPart> responsePublisher) {
        return executeHttpFetcher(ignored -> responsePublisher, null);
    }

    @SneakyThrows
    private OpenApiFetchingData executeHttpFetcherRaw(
        Function<HttpGatewayUpstreamRequest, CompletableFuture<HttpGatewayUpstreamResponse>> responseMaker,
        HttpGatewayUpstreamAuthenticator upstreamAuthenticator
    ) {
        return new OpenApiHttpFetcher(new OpenApiHttpFetcherConfiguration(
            "service-test",
            "https://remote-url",
            new HttpGatewayUpstreamClient() {
                @Override
                public CompletableFuture<HttpGatewayUpstreamResponse> executeUpstreamRequest(HttpGatewayUpstreamRequest upstreamRequest) {
                    return responseMaker.apply(upstreamRequest);
                }
            },
            upstreamAuthenticator
        ))
            .fetch()
            .get();
    }

    @SneakyThrows
    private OpenApiFetchingData executeHttpFetcher(
        Function<HttpGatewayUpstreamRequest, Publisher<HttpResponseBodyPart>> responsePublisher,
        HttpGatewayUpstreamAuthenticator upstreamAuthenticator
    ) {
        return executeHttpFetcherRaw(
            request -> {
                HttpGatewayUpstreamResponse response = makeBaseResponse();
                response.setPublisher(responsePublisher.apply(request));
                return CompletableFuture.completedFuture(response);
            },
            upstreamAuthenticator
        );
    }

    private HttpGatewayUpstreamResponse makeBaseResponse() {
        HttpGatewayUpstreamResponse response = new HttpGatewayUpstreamResponse();
        response.setContentType("charset=utf-8");
        response.setResponseStatus(HttpGatewayResponseStatus.OK);
        return response;
    }

    private Publisher<HttpResponseBodyPart> makePetstorePublisher() {
        return StaticBytesPublisher.makeBodyPartPublisher(
            readResources("/petstore.yaml")
        );
    }

    public Publisher<HttpResponseBodyPart> makeStringPublisher(String content) {
        return StaticBytesPublisher.makeBodyPartPublisher(content.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    private String makeValidOpenApi(String title) {
        return Yaml.mapper().writeValueAsString(new OpenAPI().info(new Info().title(title)));
    }

    @SneakyThrows
    private static byte[] readResources(String path) {
        //noinspection DataFlowIssue
        return Resources.toByteArray(OpenApiHttpFetcherTest.class.getResource(path));
    }
}
