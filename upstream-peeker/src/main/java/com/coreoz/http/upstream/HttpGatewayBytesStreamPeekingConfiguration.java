package com.coreoz.http.upstream;

import lombok.Value;
import play.mvc.Http;

/**
 * Configuration for {@link HttpGatewayUpstreamBytesPeekerClient}
 * @param <D> The type of the converted bytes for the downstream request body, see {@link PeekingFunction} for bytes conversion
 * @param <U> The type of the converted bytes for the upstream response body, see {@link PeekingFunction} for bytes conversion
 */
@Value
public class HttpGatewayBytesStreamPeekingConfiguration<D, U> {
    private static final HttpGatewayBytesStreamPublisherConfiguration<Object, Void> IDLE_PEEKING_PUBLISHER = new HttpGatewayBytesStreamPublisherConfiguration<>(0, (httpFrame, bytes) -> null);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final HttpGatewayBytesStreamPeekingConfiguration<Void, Void> IDLE_PEEKING_CONFIGURATION = new HttpGatewayBytesStreamPeekingConfiguration<>(
        (HttpGatewayBytesStreamPublisherConfiguration) IDLE_PEEKING_PUBLISHER,
        (HttpGatewayBytesStreamPublisherConfiguration) IDLE_PEEKING_PUBLISHER
    );
    public static <D, U> HttpGatewayBytesStreamPeekingConfiguration<D, U> newIdlePeeker() {
        //noinspection unchecked
        return (HttpGatewayBytesStreamPeekingConfiguration<D, U>) IDLE_PEEKING_CONFIGURATION;
    }

    HttpGatewayBytesStreamPublisherConfiguration<Http.Request, D> downstreamBodyKeeperConfig;
    HttpGatewayBytesStreamPublisherConfiguration<HttpGatewayUpstreamResponse, U> upstreamBodyKeeperConfig;

    /**
     * The function used to peek body bytes for a downstream request or an upstream response.<br>
     * <br>
     * This function will be called once all the bytes are peeked.<br>
     * <br>
     * This function will not be called if there is no body publisher (e.g. in a GET request).
     * @param <S> Either a {@link Http.Request} or a {@link HttpGatewayUpstreamResponse}, see {@link HttpGatewayBytesStreamPeekingConfiguration} for usage.
     * @param <T> The type in which the incoming bytes will be converted
     */
    @FunctionalInterface
    public interface PeekingFunction<S, T> {
        /**
         * @param bodyContainer This parameter value will never be null.
         * @param bytesPeeked This parameter value can be null if there is no bytes peeked (but a non-empty publisher)
         * @return The converted body, it could be for instance a {@link String}, see {@link HttpGatewayUpstreamStringPeekerClient}
         */
        T peek(S bodyContainer, byte[] bytesPeeked);
    }

    @Value
    public static class HttpGatewayBytesStreamPublisherConfiguration<S, T> {
        int maxBytesToPeek;
        PeekingFunction<S, T> peeker;
    }
}
