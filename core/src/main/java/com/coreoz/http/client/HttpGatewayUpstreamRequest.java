package com.coreoz.http.client;

import com.coreoz.http.play.HttpGatewayDownstreamRequests;
import lombok.Value;
import org.asynchttpclient.Param;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.stream.Collectors;

@Value
public class HttpGatewayUpstreamRequest {
    Http.Request downstreamRequest;
    RequestBuilder upstreamRequest;

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
}
