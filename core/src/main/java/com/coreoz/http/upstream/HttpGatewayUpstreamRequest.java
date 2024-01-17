package com.coreoz.http.upstream;

import com.coreoz.http.play.HttpGatewayDownstreamRequests;
import org.asynchttpclient.Param;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HttpGatewayUpstreamRequest {
    private final Http.Request downstreamRequest;
    private final RequestBuilder upstreamRequest;

    public HttpGatewayUpstreamRequest(Http.Request downstreamRequest, RequestBuilder upstreamRequest) {
        this.downstreamRequest = downstreamRequest;
        this.upstreamRequest = upstreamRequest;
    }

    /**
     * Set the URL of the upstream request
     * @see RequestBuilder#setUrl(String)
     */
    public HttpGatewayUpstreamRequest withUrl(String url) {
        upstreamRequest.setUrl(url);
        return this;
    }

    /**
     * Copy a header from the downstream request to the upstream request
     * @param httpHeaderName The header name to copy
     */
    public HttpGatewayUpstreamRequest copyHeader(String httpHeaderName) {
        return copyHeaders(httpHeaderName);
    }

    /**
     * Copy a header from the downstream request to the upstream request
     * @param httpHeaderNames The header names to copy
     */
    public HttpGatewayUpstreamRequest copyHeaders(String ...httpHeaderNames) {
        for (String httpHeaderName : httpHeaderNames) {
            HttpGatewayDownstreamRequests.copyHeader(downstreamRequest, upstreamRequest, httpHeaderName);
        }
        return this;
    }

    /**
     * Copy basic headers from the downstream request to the upstream request
     * @see HttpGatewayDownstreamRequests#copyBasicHeaders(Http.Request, RequestBuilder)
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

    /**
     * Change the upstream request, using optionally the values of the incoming downstream request
     */
    public HttpGatewayUpstreamRequest with(HttpGatewayRequestCustomizer customizer) {
        if (customizer != null) {
            customizer.customize(downstreamRequest, upstreamRequest);
        }
        return this;
    }

    public Http.Request getDownstreamRequest() {
        return downstreamRequest;
    }

    public RequestBuilder getUpstreamRequest() {
        return upstreamRequest;
    }

    /**
     * Function provided to {@link #with(HttpGatewayRequestCustomizer)}
     */
    @FunctionalInterface
    public interface HttpGatewayRequestCustomizer {
        /**
         * Change the upstream request, using optionally the values of the incoming downstream request
         */
        void customize(Http.Request downstreamRequest, RequestBuilder upstreamRequest);
    }
}
