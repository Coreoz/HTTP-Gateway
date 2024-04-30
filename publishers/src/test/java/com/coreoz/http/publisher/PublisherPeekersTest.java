package com.coreoz.http.publisher;

import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.HttpResponseBodyPart;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PublisherPeekersTest {
    @Test
    public void verify_that_non_peeking_publisher_argument_throws_exception() {
        try {
            PublisherPeekers.consume(s -> {});
            Assertions.fail("Should throw exception");
        } catch (Exception e) {
            Assertions.assertThat(e).isInstanceOf(IllegalArgumentException.class);
            Assertions.assertThat(e.getMessage()).isEqualTo("publisherToConsume must be an instance of PublisherPeeker");
        }
    }

    @Test
    @SneakyThrows
    public void verify_that_when_peeker_is_finished_base_publisher_consuming_is_stopped() {
        int publisherCapacity = 100;
        int sizeToPeek = 10;
        byte[] sampleBody = PublisherBytes.makeBytes(publisherCapacity);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        BytePublisherWithIndex bytePublisherWithIndex = slowBodyPartPublisher(sampleBody);
        PublisherPeeker<HttpResponseBodyPart> peekingPublisher = PublisherPeekerTest.makePeeker(bytePublisherWithIndex.publisher(), peekedBytes, sizeToPeek);
        PublisherPeekers.consume(peekingPublisher);
        Integer bytesReadIndex = bytePublisherWithIndex.finalState().get();

        Assertions.assertThat(bytesReadIndex).isEqualTo(sizeToPeek);
        Assertions.assertThat(peekedBytes.getValue()).isEqualTo(PublisherBytes.subBytes(sampleBody, 0, sizeToPeek));
    }

    @Test
    @SneakyThrows
    public void verify_that_when_peeker_capacity_is_big_enough_then_base_publisher_is_fully_consumed() {
        int publisherCapacity = 30;
        int sizeToPeek = 100;
        byte[] sampleBody = PublisherBytes.makeBytes(publisherCapacity);
        MutableObject<byte[]> peekedBytes = new MutableObject<>();
        BytePublisherWithIndex bytePublisherWithIndex = slowBodyPartPublisher(sampleBody);
        PublisherPeeker<HttpResponseBodyPart> peekingPublisher = PublisherPeekerTest.makePeeker(bytePublisherWithIndex.publisher(), peekedBytes, sizeToPeek);
        PublisherPeekers.consume(peekingPublisher);
        Integer bytesReadIndex = bytePublisherWithIndex.finalState().get();

        Assertions.assertThat(bytesReadIndex).isEqualTo(publisherCapacity);
        Assertions.assertThat(peekedBytes.getValue()).isEqualTo(sampleBody);
    }



    private BytePublisherWithIndex slowBodyPartPublisher(byte[] data) {
        AtomicInteger byteSentIndex = new AtomicInteger(0);
        CompletableFuture<Integer> finalState = new CompletableFuture<>();
        return new BytePublisherWithIndex(
            finalState,
            subscriber -> subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long bytesRequested) {
                    if (byteSentIndex.get() == data.length) {
                        subscriber.onComplete();
                        finalState.complete(byteSentIndex.get());
                        return;
                    }

                    subscriber.onNext(StaticBytesPublishers.toBodyPart(new byte[] { data[byteSentIndex.getAndIncrement()] }));
                }

                @Override
                public void cancel() {
                    finalState.complete(byteSentIndex.get());
                }
            })
        );
    }

    record BytePublisherWithIndex(CompletableFuture<Integer> finalState, Publisher<HttpResponseBodyPart> publisher) {}
}
