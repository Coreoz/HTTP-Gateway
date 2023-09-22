package com.coreoz.http.upstream;

import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.function.BiConsumer;

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


    public HttpGatewayPeekingUpstreamRequest<D, U> withUrl(String url) {
        upstreamRequest.withUrl(url);
        return this;
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> copyHeader(String httpHeaderName) {
        upstreamRequest.copyHeader(httpHeaderName);
        return this;
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> copyBasicHeaders() {
        upstreamRequest.copyBasicHeaders();
        return this;
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> copyQueryParams() {
        upstreamRequest.copyQueryParams();
        return this;
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> with(BiConsumer<Http.Request, RequestBuilder> customizer) {
        upstreamRequest.with(customizer);
        return this;
    }
}
