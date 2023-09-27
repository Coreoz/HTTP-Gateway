package com.coreoz.http.upstream;

import lombok.Value;
import play.mvc.Http;

import java.util.function.BiFunction;

@Value
public class HttpGatewayBytesStreamPeekingConfiguration<D, U> {
    private final static HttpGatewayBytesStreamPublisherConfiguration<Object, Void> IDLE_PEEKING_PUBLISHER = new HttpGatewayBytesStreamPublisherConfiguration<>(0, (httpFrame, bytes) -> null);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private final static HttpGatewayBytesStreamPeekingConfiguration<Void, Void> IDLE_PEEKING_CONFIGURATION = new HttpGatewayBytesStreamPeekingConfiguration<>(
        (HttpGatewayBytesStreamPublisherConfiguration) IDLE_PEEKING_PUBLISHER,
        (HttpGatewayBytesStreamPublisherConfiguration) IDLE_PEEKING_PUBLISHER
    );
    public static <D, U> HttpGatewayBytesStreamPeekingConfiguration<D, U> newIdlePeeker() {
        //noinspection unchecked
        return (HttpGatewayBytesStreamPeekingConfiguration<D, U>) IDLE_PEEKING_CONFIGURATION;
    }

    HttpGatewayBytesStreamPublisherConfiguration<Http.Request, D> downstreamBodyKeeperConfig;
    HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> upstreamBodyKeeperConfig;

    @Value
    public static class HttpGatewayBytesStreamPublisherConfiguration<S, T> {
        int maxBytesToPeek;
        // TODO use custom function to describe parameters
        BiFunction<S, byte[], T> peeker;
    }
}
