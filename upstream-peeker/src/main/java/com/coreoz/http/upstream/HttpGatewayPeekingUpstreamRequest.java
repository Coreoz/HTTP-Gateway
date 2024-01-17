package com.coreoz.http.upstream;

import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

/**
 * A wrapper for {@link HttpGatewayUpstreamRequest} to use {@link HttpGatewayUpstreamBytesPeekerClient}
 * @param <D> See {@link HttpGatewayBytesStreamPeekingConfiguration}
 * @param <U> See {@link HttpGatewayBytesStreamPeekingConfiguration}
 */
public class HttpGatewayPeekingUpstreamRequest<D, U> {
    private final StreamPeekersFuture<D, U> streamPeekersFuture;
    private final HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> upstreamBodyKeeperConfig;
    private final HttpGatewayUpstreamRequest upstreamRequest;

    public HttpGatewayPeekingUpstreamRequest(
        Http.Request downstreamRequest,
        RequestBuilder upstreamRequest,
        StreamPeekersFuture<D, U> streamPeekersFuture,
        HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> upstreamBodyKeeperConfig) {
        this(new HttpGatewayUpstreamRequest(downstreamRequest, upstreamRequest), streamPeekersFuture, upstreamBodyKeeperConfig);
    }

    public HttpGatewayPeekingUpstreamRequest(
        HttpGatewayUpstreamRequest upstreamRequest,
        StreamPeekersFuture<D, U> streamPeekersFuture,
        HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> upstreamBodyKeeperConfig) {
        this.upstreamRequest = upstreamRequest;
        this.streamPeekersFuture = streamPeekersFuture;
        this.upstreamBodyKeeperConfig = upstreamBodyKeeperConfig;
    }

    StreamPeekersFuture<D, U> getStreamPeekersFuture() {
        return streamPeekersFuture;
    }

    HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> getUpstreamBodyKeeperConfig() {
        return upstreamBodyKeeperConfig;
    }

    HttpGatewayUpstreamRequest getUpstreamRequest() {
        return upstreamRequest;
    }

    // Delegate calls

    /**
     * @see HttpGatewayUpstreamRequest#withUrl(String)
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> withUrl(String url) {
        upstreamRequest.withUrl(url);
        return this;
    }

    /**
     * @see HttpGatewayUpstreamRequest#copyHeader(String)
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> copyHeader(String httpHeaderName) {
        upstreamRequest.copyHeader(httpHeaderName);
        return this;
    }

    /**
     * @see HttpGatewayUpstreamRequest#copyHeaders(String...)
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> copyHeaders(String ...httpHeaderNames) {
        upstreamRequest.copyHeaders(httpHeaderNames);
        return this;
    }

    /**
     * @see HttpGatewayUpstreamRequest#copyBasicHeaders()
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> copyBasicHeaders() {
        upstreamRequest.copyBasicHeaders();
        return this;
    }

    /**
     * @see HttpGatewayUpstreamRequest#copyQueryParams()
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> copyQueryParams() {
        upstreamRequest.copyQueryParams();
        return this;
    }

    /**
     * @see HttpGatewayUpstreamRequest#with(HttpGatewayUpstreamRequest.HttpGatewayRequestCustomizer)
     */
    public HttpGatewayPeekingUpstreamRequest<D, U> with(HttpGatewayUpstreamRequest.HttpGatewayRequestCustomizer customizer) {
        upstreamRequest.with(customizer);
        return this;
    }
}
