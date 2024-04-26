package com.coreoz.http.publisher;

import com.coreoz.http.mock.SparkMockServer;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstream.HttpGatewayUpstreamRequest;
import com.coreoz.http.upstream.HttpGatewayUpstreamResponse;
import lombok.SneakyThrows;
import play.mvc.Http;

import java.util.concurrent.CompletableFuture;

public class UpstreamRequests {
    @SneakyThrows
    public static CompletableFuture<HttpGatewayUpstreamResponse> executeEchoRequest(byte[] data) {
        HttpGatewayUpstreamClient upstreamClient = new HttpGatewayUpstreamClient();
        HttpGatewayUpstreamRequest request = upstreamClient
            .prepareRequest(new Http.RequestBuilder().build())
            .withUrl("http://localhost:" + SparkMockServer.SPARK_HTTP_PORT + "/echo")
            .with(((downstreamRequest, upstreamRequest) -> {
                upstreamRequest.setBody(StaticBytesPublishers.makeByteBufPublisher(data));
                upstreamRequest.setMethod("POST");
            }));
        return upstreamClient
            .executeUpstreamRequest(request);
    }
}
