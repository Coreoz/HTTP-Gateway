package com.coreoz.http.upstream;

import com.coreoz.http.publisher.HttpCharsetParser;
import play.mvc.Http;

import java.util.concurrent.CompletableFuture;

/**
 * An upstream client like {@link HttpGatewayUpstreamClient} that provides a peeking feature for the bodies
 * of the incoming downstream request body and the remote upstream response body.<br>
 * <br>
 * The peeked streams will be interpreted as strings.<br>
 * The string charset is interpreted from the request/response content-type header. See {@link HttpCharsetParser} for details.
 * <br>
 * A raw {@code byte[]} peeker is available: {@link HttpGatewayUpstreamBytesPeekerClient}
 */
public class HttpGatewayUpstreamStringPeekerClient {
    private final HttpGatewayUpstreamBytesPeekerClient<String, String> bytesPeekerClient;

    public static final HttpGatewayBytesStreamPeekingConfiguration.PeekingFunction<Http.Request, String> DOWNSTREAM_STRING_PEEKER = (downstreamRequest, bytesPeeked) ->
        bytesPeeked == null ?
            null
            : new String(
                bytesPeeked,
                HttpCharsetParser.parseEncodingFromHttpContentType(
                    downstreamRequest.contentType().orElse(null)
                )
            );
    public static final HttpGatewayBytesStreamPeekingConfiguration.PeekingFunction<HttpGatewayUpstreamResponse, String> UPSTREAM_STRING_PEEKER = (upstreamRequest, bytesPeeked) ->
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
