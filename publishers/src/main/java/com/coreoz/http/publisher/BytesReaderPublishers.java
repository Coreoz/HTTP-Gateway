package com.coreoz.http.publisher;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Provide a way to read a {@link Publisher} from the body of a downstream request or an upstream response.
 */
public class BytesReaderPublishers {
    private static final long MAX_BYTES_TO_READ_DEFAULT = 100_000L;

    /**
     * Read all the bytes in the publisher.<br>
     * <br>
     * Important considerations:<br>
     * - After this function has been executed, the publisher cannot be read anymore. TODO StaticBytesPublisher to create another Publisher from the bytes<br>
     * - Reading a whole body of a downstream request or an upstream response will load all the data in memory. To avoid {@link OutOfMemoryError}, it is important to set a reasonable limit of data that will be read. The default is 100ko, use {@link #publisherToFutureBytes(Publisher, Function, long)} to change that.<br>
     * <br>
     * Sample usages:<br>
     * - Downstream request body: BytesReaderPublishers.publisherToFutureBytes(request.body().as(Publisher.class), ByteReaders::readBytesFromByteBuf).then(...)<br>
     * - Upstream response body: BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart).then(...)
     * @param publisher The {@link Publisher} to consume
     * @param bytesReader The function that will
     * @return The {@link CompletableFuture} with the byte array that has been read
     * @param <T> The type of data the Publisher publish
     */
    public static <T> @NotNull CompletableFuture<byte[]> publisherToFutureBytes(@NotNull Publisher<T> publisher, @NotNull Function<T, byte[]> bytesReader) {
        return publisherToFutureBytes(publisher, bytesReader, MAX_BYTES_TO_READ_DEFAULT);
    }

    /**
     * Read all the bytes in the publisher.<br>
     * <br>
     * Important considerations:<br>
     * - After this function has been executed, the publisher cannot be read anymore. TODO StaticBytesPublisher to create another Publisher from the bytes<br>
     * - Reading a whole body of a downstream request or an upstream response will load all the data in memory. To avoid {@link OutOfMemoryError}, it is important to set a reasonable limit of data that will be read in the maxBytesToRead parameter.<br>
     * <br>
     * Sample usages:<br>
     * - Downstream request body: BytesReaderPublishers.publisherToFutureBytes(request.body().as(Publisher.class), ByteReaders::readBytesFromByteBuf).then(...)<br>
     * - Upstream response body: BytesReaderPublishers.publisherToFutureBytes(response.getPublisher(), ByteReaders::readBytesFromHttpResponseBodyPart).then(...)
     * @param publisher The {@link Publisher} to consume
     * @param bytesReader The function that will
     * @param maxBytesToRead The max number of bytes to read (it should generally not be higher than 100ko)
     * @return The {@link CompletableFuture} with the byte array that has been read
     * @param <T> The type of data the Publisher publish
     */
    public static <T> @NotNull CompletableFuture<byte[]> publisherToFutureBytes(@NotNull Publisher<T> publisher, @NotNull Function<T, byte[]> bytesReader, long maxBytesToRead) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ByteArrayOutputStream bytesArray = new ByteArrayOutputStream();
        SubscriptionHolder subscription = new SubscriptionHolder();
        // TODO make some tests to see how many bytes is read each time to adjust the ratio
        long partsToRequest = maxBytesToRead / 8;
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscription.setSubscription(s);
                s.request(partsToRequest);
            }

            @Override
            public void onNext(T data) {
                byte[] bytesRead = bytesReader.apply(data);
                long totalBytesLength = bytesArray.size() + bytesRead.length;
                if (totalBytesLength <= maxBytesToRead) {
                    bytesArray.writeBytes(bytesRead);
                    subscription.getSubscription().request(partsToRequest);
                } else {
                    // TODO unit test all this
                    subscription.getSubscription().cancel();
                    future.completeExceptionally(new RuntimeException(
                        "Tried to read at least " + totalBytesLength + " bytes of data whereas the limit was set as " + maxBytesToRead
                    ));
                }
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
