package com.coreoz.client;

import com.coreoz.play.HttpGatewayRequests;
import lombok.Value;
import org.asynchttpclient.Param;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.stream.Collectors;

@Value
public class HttpGatewayRemoteRequest {
    Http.Request incomingRequest;
    RequestBuilder remoteRequest;

    public HttpGatewayRemoteRequest copyHeader(String httpHeader) {
        HttpGatewayRequests.copyHeader(incomingRequest, remoteRequest, httpHeader);
        return this;
    }

    public HttpGatewayRemoteRequest copyBasicHeaders() {
        HttpGatewayRequests.copyBasicHeaders(incomingRequest, remoteRequest);
        return this;
    }

    public HttpGatewayRemoteRequest copyQueryParams() {
        remoteRequest.setQueryParams(
            incomingRequest.queryString()
                .entrySet()
                .stream()
                .flatMap(entry ->
                    Arrays
                        .stream(entry.getValue())
                        .map(value -> new Param(entry.getKey(), value))
                )
                .collect(Collectors.toList())
        );
    }
}
