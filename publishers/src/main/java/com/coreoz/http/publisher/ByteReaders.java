package com.coreoz.http.publisher;

import io.netty.buffer.ByteBuf;
import org.asynchttpclient.HttpResponseBodyPart;
import org.jetbrains.annotations.NotNull;

/**
 * Bytes readers for Publishers created by Netty and Asynchttpclient
 * @see PublisherPeeker
 */
public class ByteReaders {
    public static byte @NotNull [] readBytesFromByteBuf(@NotNull ByteBuf byteBuf) {
        if (byteBuf.hasArray()) {
            return byteBuf.array();
        } else {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            return bytes;
        }
    }

    public static byte @NotNull [] readBytesFromHttpResponseBodyPart(@NotNull HttpResponseBodyPart bodyPart) {
        return bodyPart.getBodyPartBytes();
    }
}
