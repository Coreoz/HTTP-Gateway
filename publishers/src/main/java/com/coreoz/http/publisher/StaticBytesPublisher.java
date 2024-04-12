package com.coreoz.http.publisher;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledHeapByteBuf;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.netty.EagerResponseBodyPart;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// TODO try to use this with Play and AHC
public class StaticBytesPublisher {
    public static Publisher<ByteBuf> makeByteBufPublisher(byte[] data) {
        return makeBytesPublisher(data, StaticBytesPublisher::toByteBuf);
    }

    public static Publisher<HttpResponseBodyPart> makeBodyPartPublisher(byte[] data) {
        return makeBytesPublisher(data, StaticBytesPublisher::toBodyPart);
    }

    private static <T> Publisher<T> makeBytesPublisher(byte[] data, Function<byte[], T> bytesWrapper) {
        // ByteReaderCounter byteReaderCounter = new ByteReaderCounter(data, 0L);
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long bytesRequested) {
                if (isCompleted.get()) {
                    return;
                }
                // byte[] bytesRead = readBytes(byteReaderCounter, bytesRequested);
                // if (bytesRead != null) {
                //    subscriber.onNext(bytesWrapper.apply(bytesRead));
                // }
                isCompleted.set(true);
                subscriber.onNext(bytesWrapper.apply(data));
                // if (byteReaderCounter.isCompleted()) {
                    subscriber.onComplete();
                // }
            }

            @Override
            public void cancel() {
            }
        });
    }

    private static byte[] readBytes(ByteReaderCounter byteReaderCounter, long bytesRequested) {
        if (byteReaderCounter.isCompleted()) {
            return null;
        }
        return null;
    }

    private static ByteBuf toByteBuf(byte[] data) {
        return new UnpooledHeapByteBuf(new PooledByteBufAllocator(), data, data.length) {};
    }

    private static HttpResponseBodyPart toBodyPart(byte[] data) {
        return new EagerResponseBodyPart(toByteBuf(data), true);
    }

    record ByteReaderCounter(byte[] data, long bytesRead) {
        boolean isCompleted() {
            return bytesRead == data.length;
        }
    }
}
