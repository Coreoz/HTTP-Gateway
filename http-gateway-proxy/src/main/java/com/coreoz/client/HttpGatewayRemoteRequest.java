package com.coreoz.client;

import com.coreoz.play.HttpGatewayRequests;
import io.netty.buffer.ByteBuf;
import lombok.Value;
import org.asynchttpclient.RequestBuilder;
import org.reactivestreams.Publisher;
import play.mvc.Http;

@Value
public class HttpGatewayRemoteRequest {
    Http.Request incomingRequest;
    RequestBuilder remoteRequest;

    public HttpGatewayRemoteRequest copyHeader(String httpHeader) {
        HttpGatewayRequests.copyHeader(incomingRequest, remoteRequest, httpHeader);
        return this;
    }

    public HttpGatewayRemoteRequest copyBasicHeaders() {
        HttpGatewayRequests.copyBasicHeaders(incomingRequest, remoteRequest);
        return this;
    }
}
