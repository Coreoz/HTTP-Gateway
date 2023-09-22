package com.coreoz.http.upstream;

import java.util.concurrent.CompletableFuture;

public class StreamPeekersFuture<D, U>  {
    private D downstreamBodyProcessed;
    private boolean isDownstreamBodyProcessed;
    private U upstreamBodyProcessed;
    private boolean isUpstreamBodyProcessed;
    private final CompletableFuture<HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<D, U>> streamsPeekedFuture;

    public StreamPeekersFuture() {
        this.streamsPeekedFuture = new CompletableFuture<>();
    }

    void downstreamBodyProcessed(D downstreamBodyProcessed) {
        this.downstreamBodyProcessed = downstreamBodyProcessed;
        this.isDownstreamBodyProcessed = true;
        completeFutureIfDownstreamAndUpstreamProcessed();
    }

    void upstreamBodyProcessed(U upstreamBodyProcessed) {
        this.upstreamBodyProcessed = upstreamBodyProcessed;
        this.isUpstreamBodyProcessed = true;
        completeFutureIfDownstreamAndUpstreamProcessed();
    }

    CompletableFuture<HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<D, U>> getStreamsPeekedFuture() {
        return streamsPeekedFuture;
    }

    private void completeFutureIfDownstreamAndUpstreamProcessed() {
        if (isDownstreamBodyProcessed && isUpstreamBodyProcessed) {
            streamsPeekedFuture.complete(new HttpGatewayUpstreamKeepingResponse.HttpGatewayUpstreamPeeked<>(
                downstreamBodyProcessed,
                upstreamBodyProcessed
            ));
        }
    }
}
