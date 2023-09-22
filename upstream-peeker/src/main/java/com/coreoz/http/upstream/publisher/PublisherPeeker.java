package com.coreoz.http.upstream.publisher;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Proxy publisher that will intercept part of the data that are being published by the original publisher
 */
@Slf4j
public class PublisherPeeker<T> implements Publisher<T> {
    private final Publisher<T> publisher;
    private final Consumer<byte[]> onPeek;
    private final Function<T, byte[]> bytesReader;
    private final int maxBytesToPeek;
    private final ByteArrayOutputStream bytesBuffer;
    private boolean isPeekingFinished;

    /**
     * @param publisher      The original publisher
     * @param onPeek         Called either when the publisher has finished publishing or if the maxBytesToPeek has been reached
     * @param bytesReader    The function that will be able to read bytes from the original publisher
     * @param maxBytesToPeek The max number of bytes to peek
     */
    public PublisherPeeker(Publisher<T> publisher, Consumer<byte[]> onPeek, Function<T, byte[]> bytesReader, int maxBytesToPeek) {
        this.publisher = Preconditions.checkNotNull(publisher);
        this.onPeek = onPeek;
        this.bytesReader = bytesReader;
        this.maxBytesToPeek = maxBytesToPeek;
        this.bytesBuffer = new ByteArrayOutputStream();
        this.isPeekingFinished = false;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        publisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        logger.trace("{} blocks of data requested", n);
                        subscription.request(n);
                    }

                    @Override
                    public void cancel() {
                        logger.trace("Subscription cancelled");
                        subscription.cancel();
                        // Dans le cas d'une réponse sans contenu (204 No Content), alors onComplete n'est pas appelé
                        PublisherPeeker.this.terminatePeeking();
                    }
                });
            }

            @Override
            public void onNext(T t) {
                PublisherPeeker.this.handleNewIncomingData(bytesReader.apply(t));
                s.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("Error handling body data", t);

                s.onError(t);
            }

            @Override
            public void onComplete() {
                logger.trace("Body data correctly completed");
                PublisherPeeker.this.terminatePeeking();

                s.onComplete();
            }
        });
    }

    private void handleNewIncomingData(byte[] data) {
        int bytesBufferSize = bytesBuffer.size();
        if (bytesBufferSize + data.length >= maxBytesToPeek) {
            bytesBuffer.write(data, 0, maxBytesToPeek - bytesBuffer.size());
            terminatePeeking();
        } else {
            bytesBuffer.writeBytes(data);
        }
    }

    private void terminatePeeking() {
        if (isPeekingFinished) {
            return;
        }
        isPeekingFinished = true;
        if (bytesBuffer.size() == 0) {
            onPeek.accept(null);
        } else {
            onPeek.accept(bytesBuffer.toByteArray());
        }
    }

    boolean isPeekingFinished() {
        return isPeekingFinished;
    }
}
