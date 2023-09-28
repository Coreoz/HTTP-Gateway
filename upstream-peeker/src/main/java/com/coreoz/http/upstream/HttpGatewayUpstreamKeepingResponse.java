package com.coreoz.http.upstream;

import lombok.Value;

import java.util.concurrent.CompletableFuture;

/**
 * The upstream response with the peeked body from the downstream request and the upstream response.<br>
 * <br>
 * The {@code streamsPeeked} {@code CompletableFuture} will be resolved once both body stream have been peeked
 * @param <D> See {@link HttpGatewayBytesStreamPeekingConfiguration}
 * @param <U> See {@link HttpGatewayBytesStreamPeekingConfiguration}
 */
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
