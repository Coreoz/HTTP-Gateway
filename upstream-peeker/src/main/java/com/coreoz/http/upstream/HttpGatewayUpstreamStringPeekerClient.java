package com.coreoz.http.upstream;

import com.coreoz.http.upstream.publisher.HttpCharsetParser;
import play.mvc.Http;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class HttpGatewayUpstreamStringPeekerClient {
    private final HttpGatewayUpstreamBytesPeekerClient<String, String> bytesPeekerClient;

    public static final BiFunction<Http.Request, byte[], String> DOWNSTREAM_STRING_PEEKER = (downstreamRequest, bytesPeeked) ->
        bytesPeeked == null ?
            null
            : new String(
                bytesPeeked,
                HttpCharsetParser.parseEncodingFromHttpContentType(
                    downstreamRequest.contentType().orElse(null)
                )
            );
    public static final BiFunction<HttpGatewayUpstreamResponse, byte[], String> UPSTREAM_STRING_PEEKER = (upstreamRequest, bytesPeeked) ->
        bytesPeeked == null ?
            null
            : new String(
                bytesPeeked,
                HttpCharsetParser.parseEncodingFromHttpContentType(upstreamRequest.getContentType())
            );

    public HttpGatewayUpstreamStringPeekerClient(
        HttpGatewayStringStreamPeekingConfiguration defaultConfiguration,
        HttpGatewayUpstreamClient upstreamClient
    ) {
        this.bytesPeekerClient = new HttpGatewayUpstreamBytesPeekerClient<>(
            makeBytesStreamConfiguration(defaultConfiguration),
            upstreamClient
        );
    }

    private static HttpGatewayBytesStreamPeekingConfiguration<String, String> makeBytesStreamConfiguration(HttpGatewayStringStreamPeekingConfiguration configuration) {
        return new HttpGatewayBytesStreamPeekingConfiguration<>(
            new HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<>(
                configuration.getMaxBytesToPeek(),
                DOWNSTREAM_STRING_PEEKER
            ),
            new HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<>(
                configuration.getMaxBytesToPeek(),
                UPSTREAM_STRING_PEEKER
            )
        );
    }

    public HttpGatewayUpstreamStringPeekerClient(HttpGatewayStringStreamPeekingConfiguration defaultConfiguration) {
        this(defaultConfiguration, new HttpGatewayUpstreamClient());
    }

    public HttpGatewayUpstreamStringPeekerClient() {
        this(HttpGatewayStringStreamPeekingConfiguration.DEFAULT_CONFIG);
    }

    public HttpGatewayPeekingUpstreamRequest<String, String> prepareRequest(Http.Request downstreamRequest) {
        return bytesPeekerClient.prepareRequest(downstreamRequest);
    }

    public HttpGatewayPeekingUpstreamRequest<String, String> prepareRequest(Http.Request downstreamRequest, HttpGatewayStringStreamPeekingConfiguration configuration) {
        return bytesPeekerClient.prepareRequest(downstreamRequest, makeBytesStreamConfiguration(configuration));
    }

    public CompletableFuture<HttpGatewayUpstreamKeepingResponse<String, String>> executeUpstreamRequest(HttpGatewayPeekingUpstreamRequest<String, String> remoteRequest) {
        return bytesPeekerClient.executeUpstreamRequest(remoteRequest);
    }
}
