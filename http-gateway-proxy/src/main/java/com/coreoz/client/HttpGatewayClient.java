package com.coreoz.client;

import com.coreoz.play.HttpGatewayRequests;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.RequestBuilder;
import org.reactivestreams.Publisher;
import play.mvc.Http;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpGatewayClient {
    private final AsyncHttpClient asyncHttpClient;

    public HttpGatewayClient(DefaultAsyncHttpClientConfig asyncHttpClientConfig) {
        this.asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);
    }

    public HttpGatewayClient(Duration connectionTimeout, Duration readTimeout) {
        this(new DefaultAsyncHttpClientConfig.Builder()
            .setRequestTimeout((int) connectionTimeout.toMillis())
            .setReadTimeout((int) readTimeout.toMillis())
            .setCookieStore(null)
            .build());
    }

    /**
     * Create a new client with 5 minutes for connect and read timeouts
     */
    public HttpGatewayClient() {
        this(Duration.ofMinutes(5), Duration.ofMinutes(5));
    }

    /**
     * Create a new remote request from an incoming HTTP Gateway request
     * @param incomingHttpRequest The incoming request
     * @return A request builder
     */
    public HttpGatewayRemoteRequest prepareRequest(Http.Request incomingHttpRequest) {
        return new HttpGatewayRemoteRequest(
            incomingHttpRequest,
            new RequestBuilder(incomingHttpRequest.method()),
            incomingHttpRequest.body().as(Publisher.class),
            HttpGatewayRequests.parsePlayRequestContentLength(incomingHttpRequest)
        );
    }

    public CompletableFuture<HttpGatewayRemoteResponse> executeRemoteRequest(HttpGatewayRemoteRequest remoteRequest) {
        // TODO
        return null;
    }
}
