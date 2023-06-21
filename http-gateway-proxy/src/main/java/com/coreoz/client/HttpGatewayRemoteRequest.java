package com.coreoz.client;

import io.netty.buffer.ByteBuf;
import lombok.Value;
import org.asynchttpclient.RequestBuilder;
import org.reactivestreams.Publisher;

@Value
public class HttpGatewayRemoteRequest {
    RequestBuilder baseRemoteRequest;
    Publisher<ByteBuf> body;
    long contentLength;
}
