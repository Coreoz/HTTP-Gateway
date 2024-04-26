package com.coreoz.http.publisher;

import com.coreoz.http.mock.SparkMockServer;
import com.google.common.primitives.Bytes;
import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.netty.EagerResponseBodyPart;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.coreoz.http.publisher.StaticBytesPublishers.toByteBuf;

public class PublisherPeekerTest {
    static {
        SparkMockServer.initialize();
    }

    @SneakyThrows
    @Test
    public void when_peeker_capacity_bigger_than_publisher_every_bytes_must_be_read() {
        int bodySize = 100;
        byte[] sampleBody = PublisherBytes.makeBytes(bodySize);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        byte[] sampleResponse = UpstreamRequests
            .executeEchoRequest(sampleBody)
            .thenCompose(response -> BytesReaderPublishers.publisherToFutureBytes(
                makePeeker(response.getPublisher(), peekedBytes, bodySize),
                ByteReaders::readBytesFromHttpResponseBodyPart,
                bodySize
            ))
            .get();

        Assertions
            .assertThat(peekedBytes.getValue())
            .isEqualTo(sampleResponse)
            .isEqualTo(sampleBody);
    }

    @SneakyThrows
    @Test
    public void when_peeker_capacity_lower_than_publisher_only_capacity_bytes_must_be_read() {
        int bodySize = 100;
        byte[] sampleBody = PublisherBytes.makeBytes(bodySize);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        byte[] sampleResponse = UpstreamRequests
            .executeEchoRequest(sampleBody)
            .thenCompose(response -> BytesReaderPublishers.publisherToFutureBytes(
                makePeeker(response.getPublisher(), peekedBytes, bodySize - 1),
                ByteReaders::readBytesFromHttpResponseBodyPart,
                bodySize
            ))
            .get();

        Assertions
            .assertThat(Bytes.concat(peekedBytes.getValue(), new byte[] { sampleBody[sampleBody.length - 1] }))
            .isEqualTo(sampleResponse)
            .isEqualTo(sampleBody);
    }

    @SneakyThrows
    @Test
    public void when_parent_publisher_cancelled_peeker_must_peek_bytes_that_have_been_read() {
        int bodySize = 2000;
        byte[] sampleBody = PublisherBytes.makeBytes(bodySize);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        byte[] sampleResponse = UpstreamRequests
            .executeEchoRequest(sampleBody)
            .thenCompose(response -> {
                PublisherPeeker<HttpResponseBodyPart> peekerPublisher = makePeeker(response.getPublisher(), peekedBytes, bodySize);
                ByteArrayOutputStream readBytes = new ByteArrayOutputStream();
                MutableObject<Subscription> subscription = new MutableObject<>();
                CompletableFuture<byte[]> future = new CompletableFuture<>();
                peekerPublisher.subscribe(new Subscriber<>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(1);
                        subscription.setValue(s);
                    }

                    @Override
                    public void onNext(HttpResponseBodyPart httpResponseBodyPart) {
                        readBytes.writeBytes(httpResponseBodyPart.getBodyPartBytes());
                        subscription.getValue().cancel();
                        future.complete(readBytes.toByteArray());
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        future.complete(readBytes.toByteArray());
                    }
                });
                return future;
            })
            .get();

        Assertions.assertThat(sampleResponse).hasSizeLessThan(bodySize);
        Assertions
            .assertThat(peekedBytes.getValue())
            .isEqualTo(sampleResponse);
        Assertions.assertThat(Bytes.indexOf(sampleBody, sampleResponse)).isEqualTo(0);
    }

    @SneakyThrows
    @Test
    public void when_base_publisher_raise_an_error_peeker_must_propagate_the_error_and_read_bytes_returned_before_the_error() {
        byte[] sampleBody = PublisherBytes.makeBytes(20);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        String mockedErrorMessage = "Mock publisher excepted error";
        AtomicBoolean completed = new AtomicBoolean(false);
        Publisher<HttpResponseBodyPart> publisher = subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long bytesRequested) {
                if (completed.compareAndSet(false, true)) {
                    subscriber.onNext(new EagerResponseBodyPart(toByteBuf(sampleBody), false));
                    subscriber.onError(new RuntimeException(mockedErrorMessage));
                }
            }

            @Override
            public void cancel() {
            }
        });
        PublisherPeeker<HttpResponseBodyPart> peekerPublisher = makePeeker(publisher, peekedBytes, 100);
        byte[] resolvedBytes = BytesReaderPublishers
            .publisherToFutureBytes(peekerPublisher, ByteReaders::readBytesFromHttpResponseBodyPart)
            .exceptionally(exception -> exception.getMessage().getBytes())
            .get();

        Assertions.assertThat(resolvedBytes).asString().isEqualTo(mockedErrorMessage);
        Assertions
            .assertThat(peekedBytes.getValue())
            .isEqualTo(sampleBody);
    }

    private PublisherPeeker<HttpResponseBodyPart> makePeeker(Publisher<HttpResponseBodyPart> publisher, MutableObject<byte[]> peekedBytes, int maxSize) {
        return new PublisherPeeker<>(
            publisher,
            peekedBytes::setValue,
            ByteReaders::readBytesFromHttpResponseBodyPart,
            maxSize
        );
    }
}
