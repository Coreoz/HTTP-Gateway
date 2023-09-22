package com.coreoz.http.upstream;

import com.coreoz.http.upstream.publisher.ByteReaders;
import com.coreoz.http.upstream.publisher.PublisherPeeker;
import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import play.mvc.Http;

import java.util.concurrent.CompletableFuture;

public class HttpGatewayUpstreamBytesPeekerClient<D, U> {
    private final HttpGatewayBytesStreamPeekingConfiguration<D, U> defaultConfiguration;
    private final HttpGatewayUpstreamClient upstreamClient;

    public HttpGatewayUpstreamBytesPeekerClient(
        HttpGatewayBytesStreamPeekingConfiguration<D, U> defaultConfiguration,
        HttpGatewayUpstreamClient upstreamClient
    ) {
        this.defaultConfiguration = defaultConfiguration;
        this.upstreamClient = upstreamClient;
    }

    public HttpGatewayUpstreamBytesPeekerClient(HttpGatewayBytesStreamPeekingConfiguration<D, U> defaultConfiguration) {
        this(defaultConfiguration, new HttpGatewayUpstreamClient());
    }

    public HttpGatewayUpstreamBytesPeekerClient() {
        this(HttpGatewayBytesStreamPeekingConfiguration.newIdlePeeker());
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> prepareRequest(Http.Request downstreamRequest) {
        return prepareRequest(downstreamRequest, defaultConfiguration);
    }

    public HttpGatewayPeekingUpstreamRequest<D, U> prepareRequest(Http.Request downstreamRequest, HttpGatewayBytesStreamPeekingConfiguration<D, U> peekingConfiguration) {
        HttpGatewayBytesStreamPeekingConfiguration.HttpGatewayBytesStreamPublisherConfiguration<Http.Request, D> requestPeekingConfig = peekingConfiguration.getDownstreamBodyKeeperConfig();
        StreamPeekersFuture<D, U> streamPeekersFuture = new StreamPeekersFuture<>();
        @SuppressWarnings("unchecked")
        Publisher<ByteBuf> requestPublisher = downstreamRequest.body().as(Publisher.class);

        if (requestPublisher == null) {
            streamPeekersFuture.downstreamBodyProcessed(null);
        } else {
            requestPublisher = new PublisherPeeker<>(
                requestPublisher,
                requestBodyFirstBytes -> streamPeekersFuture.downstreamBodyProcessed(
                    requestPeekingConfig.getPeeker().apply(downstreamRequest, requestBodyFirstBytes)
                ),
                ByteReaders::readBytesFromByteBuf,
                requestPeekingConfig.getMaxBytesToPeek()
            );
        }

        return new HttpGatewayPeekingUpstreamRequest<>(
            upstreamClient.prepareRequest(downstreamRequest, requestPublisher),
            streamPeekersFuture,
            peekingConfiguration.getUpstreamBodyKeeperConfig()
        );
    }

    public CompletableFuture<HttpGatewayUpstreamKeepingResponse<D, U>> executeUpstreamRequest(HttpGatewayPeekingUpstreamRequest<D, U> remoteRequest) {
        return upstreamClient
            .executeUpstreamRequest(remoteRequest.getUpstreamRequest())
            .thenApply(upstreamResponse -> {
                StreamPeekersFuture<D, U> streamPeekersFuture = remoteRequest.getStreamPeekersFuture();
                if (upstreamResponse.getPublisher() == null) {
                    streamPeekersFuture.upstreamBodyProcessed(null);
                } else {
                    upstreamResponse.setPublisher(new PublisherPeeker<>(
                        upstreamResponse.getPublisher(),
                        responseBodyFirstBytes -> remoteRequest.getStreamPeekersFuture().upstreamBodyProcessed(
                            remoteRequest.getUpstreamBodyKeeperConfig().getPeeker().apply(upstreamResponse, responseBodyFirstBytes)
                        ),
                        ByteReaders::readBytesFromHttpResponseBodyPart,
                        remoteRequest.getUpstreamBodyKeeperConfig().getMaxBytesToPeek()
                    ));
                }

               return new HttpGatewayUpstreamKeepingResponse<>(upstreamResponse, streamPeekersFuture.getStreamsPeekedFuture());
            });
    }
}
