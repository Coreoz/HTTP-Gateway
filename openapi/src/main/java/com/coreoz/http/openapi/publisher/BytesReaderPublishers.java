package com.coreoz.http.openapi.publisher;

import lombok.Getter;
import lombok.Setter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BytesReaderPublishers {
    private static final long BIG_ENOUGH_NUMBER = 1000L;

    public static <T> CompletableFuture<byte[]> publisherToFutureBytes(Publisher<T> publisher, Function<T, byte[]> bytesReader) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ByteArrayOutputStream bytesArray = new ByteArrayOutputStream();
        SubscriptionHolder subscription = new SubscriptionHolder();
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription.setSubscription(s);
                s.request(BIG_ENOUGH_NUMBER);
            }

            @Override
            public void onNext(T data) {
                bytesArray.writeBytes(bytesReader.apply(data));
                subscription.getSubscription().request(BIG_ENOUGH_NUMBER);
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                future.complete(bytesArray.toByteArray());
            }
        });

        return future;
    }

    @Getter
    @Setter
    private static class SubscriptionHolder {
        private Subscription subscription;
    }
}
