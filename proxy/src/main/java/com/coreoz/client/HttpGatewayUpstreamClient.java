package com.coreoz.client;

import com.coreoz.play.HttpGatewayDownstreamRequests;
import io.netty.buffer.ByteBuf;
import org.asynchttpclient.*;
import org.reactivestreams.Publisher;
import play.mvc.Http;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The Gateway client to execute a request to a remote API (upstream request)
 * from an incoming client request (downstream request)
 */
public class HttpGatewayUpstreamClient {
    private final AsyncHttpClient asyncHttpClient;

    public HttpGatewayUpstreamClient(DefaultAsyncHttpClientConfig asyncHttpClientConfig) {
        this.asyncHttpClient = new DefaultAsyncHttpClient(asyncHttpClientConfig);
    }

    public HttpGatewayUpstreamClient(Duration connectionTimeout, Duration readTimeout) {
        this(new DefaultAsyncHttpClientConfig.Builder()
            .setRequestTimeout((int) connectionTimeout.toMillis())
            .setReadTimeout((int) readTimeout.toMillis())
            .setCookieStore(null)
            .build());
    }

    /**
     * Create a new client with 5 minutes for connect and read timeouts
     */
    public HttpGatewayUpstreamClient() {
        this(Duration.ofMinutes(5), Duration.ofMinutes(5));
    }

    /**
     * Create a new remote request from an incoming HTTP Gateway request.
     * The request body along with the request content-length header will be forwarded to the remote request.
     * @param downstreamHttpRequest The incoming request
     * @return A request builder with the body and content-length header already set
     */
    public HttpGatewayUpstreamRequest prepareRequest(Http.Request downstreamHttpRequest) {
        Publisher<ByteBuf> requestBody = downstreamHttpRequest.body().as(Publisher.class);
        long requestContentLength = HttpGatewayDownstreamRequests.parsePlayRequestContentLength(downstreamHttpRequest);
        RequestBuilder upstreamRequestBuilder = new RequestBuilder(downstreamHttpRequest.method());

        if (requestBody != null) {
            if (requestContentLength >= 0) {
                upstreamRequestBuilder.setBody(requestBody, requestContentLength);
            } else {
                upstreamRequestBuilder.setBody(requestBody);
            }
        }

        return new HttpGatewayUpstreamRequest(
            downstreamHttpRequest,
            upstreamRequestBuilder
        );
    }

    public CompletableFuture<HttpGatewayUpstreamResponse> executeUpstreamRequest(HttpGatewayUpstreamRequest upstreamRequest) {
        Request request = upstreamRequest.getUpstreamRequest().build();
        CompletableFuture<HttpGatewayUpstreamResponse> result = new CompletableFuture<>();

        asyncHttpClient.executeRequest(
            request,
            new HttpGatewayClientUpstreamResponseHandler(
                result,
                request.getUrl()
            )
        );

        return result;
    }
}
