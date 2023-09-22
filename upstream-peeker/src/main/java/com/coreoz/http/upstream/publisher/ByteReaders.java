package com.coreoz.http.upstream.publisher;

import io.netty.buffer.ByteBuf;
import org.asynchttpclient.HttpResponseBodyPart;

/**
 * Bytes readers for Publishers created by Netty and Asynchttpclient
 * @see PublisherPeeker
 */
public class ByteReaders {

    public static byte[] readBytesFromByteBuf(ByteBuf byteBuf) {
        if (byteBuf.hasArray()) {
            return byteBuf.array();
        } else {
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);
            return bytes;
        }
    }

    public static byte[] readBytesFromHttpResponseBodyPart(HttpResponseBodyPart bodyPart) {
        return bodyPart.getBodyPartBytes();
    }

}
