package com.coreoz.http.upstream;

import com.coreoz.http.play.HttpGatewayDownstreamRequests;
import org.asynchttpclient.Param;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class HttpGatewayUpstreamRequest {
    private final Http.Request downstreamRequest;
    private final RequestBuilder upstreamRequest;

    public HttpGatewayUpstreamRequest(Http.Request downstreamRequest, RequestBuilder upstreamRequest) {
        this.downstreamRequest = downstreamRequest;
        this.upstreamRequest = upstreamRequest;
    }

    public HttpGatewayUpstreamRequest withUrl(String url) {
        upstreamRequest.setUrl(url);
        return this;
    }

    /**
     * Copy a header from the downstream request to the upstream request
     * @param httpHeaderName The header name to copy
     */
    public HttpGatewayUpstreamRequest copyHeader(String httpHeaderName) {
        HttpGatewayDownstreamRequests.copyHeader(downstreamRequest, upstreamRequest, httpHeaderName);
        return this;
    }

    /**
     * Copy basic headers from the downstream request to the upstream request
     */
    public HttpGatewayUpstreamRequest copyBasicHeaders() {
        HttpGatewayDownstreamRequests.copyBasicHeaders(downstreamRequest, upstreamRequest);
        return this;
    }

    /**
     * Copy query params from the downstream request to the upstream request
     */
    public HttpGatewayUpstreamRequest copyQueryParams() {
        upstreamRequest.setQueryParams(
            downstreamRequest.queryString()
                .entrySet()
                .stream()
                .flatMap(entry ->
                    Arrays
                        .stream(entry.getValue())
                        .map(value -> new Param(entry.getKey(), value))
                )
                .collect(Collectors.toList())
        );
        return this;
    }

    public HttpGatewayUpstreamRequest with(BiConsumer<Http.Request, RequestBuilder> customizer) {
        if (customizer != null) {
            customizer.accept(downstreamRequest, upstreamRequest);
        }
        return this;
    }

    public Http.Request getDownstreamRequest() {
        return downstreamRequest;
    }

    public RequestBuilder getUpstreamRequest() {
        return upstreamRequest;
    }
}
