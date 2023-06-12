package com.coreoz.client;

import io.netty.buffer.ByteBuf;
import org.asynchttpclient.RequestBuilder;
import org.reactivestreams.Publisher;

public class HttpGatewayRemoteRequest {
    RequestBuilder baseRemoteRequest;
    Publisher<ByteBuf> body;
    long contentLength;
}
