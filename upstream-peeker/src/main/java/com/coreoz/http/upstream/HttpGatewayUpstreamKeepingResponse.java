package com.coreoz.http.upstream;

import lombok.Value;

import java.util.concurrent.CompletableFuture;

@Value
public class HttpGatewayUpstreamKeepingResponse<D, U> {
    HttpGatewayUpstreamResponse upstreamResponse;
    CompletableFuture<HttpGatewayUpstreamPeeked<D, U>> streamsPeeked;

    @Value
    public static class HttpGatewayUpstreamPeeked<D, U> {
        D downstreamPeeking;
        U upstreamPeeking;
    }
}
