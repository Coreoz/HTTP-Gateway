package com.coreoz.http.publisher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.netty.EagerResponseBodyPart;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Provide a way to create a {@link Publisher} from a byte array.<br>
 * <br>
 * To extract byte array from an existing publisher, see {@link BytesReaderPublishers}.
 */
public class StaticBytesPublishers {
    /**
     * Create a new {@link ByteBuf} {@link Publisher} from a byte array
     * @param data The byte array that will feed the publisher
     * @return The new publisher
     */
    public static Publisher<ByteBuf> makeByteBufPublisher(byte[] data) {
        return makeBytesPublisher(data, StaticBytesPublishers::toByteBuf);
    }

    /**
     * Create a new {@link HttpResponseBodyPart} {@link Publisher} from a byte array
     * @param data The byte array that will feed the publisher
     * @return The new publisher
     */
    public static Publisher<HttpResponseBodyPart> makeBodyPartPublisher(byte[] data) {
        return makeBytesPublisher(data, StaticBytesPublishers::toBodyPart);
    }

    private static <T> Publisher<T> makeBytesPublisher(byte[] data, Function<byte[], T> bytesWrapper) {
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long bytesRequested) {
                if (isCompleted.get()) {
                    return;
                }

                isCompleted.set(true);
                subscriber.onNext(bytesWrapper.apply(data));
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                // nothing to cancel because the request() function will consume directly all the bytes
            }
        });
    }

    static ByteBuf toByteBuf(byte[] data) {
        return new UnpooledHeapByteBuf(new PooledByteBufAllocator(), data, data.length) {};
    }

    static HttpResponseBodyPart toBodyPart(byte[] data) {
        return new EagerResponseBodyPart(toByteBuf(data), true);
    }
}
