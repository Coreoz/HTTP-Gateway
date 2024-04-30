package com.coreoz.http.publisher;

import com.google.common.primitives.Bytes;

import java.util.Random;

public class PublisherBytes {
    public static int DEFAULT_BYTES_SIZE = 8_000_000;

    public static byte[] makeBytes() {
        return makeBytes(DEFAULT_BYTES_SIZE);
    }

    public static byte[] makeBytes(int size) {
        byte[] startBytes = "start".getBytes();
        byte[] endBytes = "end".getBytes();

        Random random = new Random();
        byte[] randomBytes = new byte[size - startBytes.length - endBytes.length];
        random.nextBytes(randomBytes);

        return Bytes.concat(
            startBytes,
            randomBytes,
            endBytes
        );
    }

    public static byte[] subBytes(byte[] bytes, int start, int lengthToCopy) {
        byte[] subBytes = new byte[lengthToCopy];
        System.arraycopy(bytes, start, subBytes, 0, lengthToCopy);
        return subBytes;
    }
}
