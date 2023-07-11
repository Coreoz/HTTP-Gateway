package com.coreoz.client;

import com.coreoz.play.HttpGatewayRequests;
import io.netty.buffer.ByteBuf;
import org.asynchttpclient.*;
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
     * Create a new remote request from an incoming HTTP Gateway request.
     * The request body along with the request content-length header will be forwarded to the remote request.
     * @param incomingHttpRequest The incoming request
     * @return A request builder with the body and content-length header already set
     */
    public HttpGatewayRemoteRequest prepareRequest(Http.Request incomingHttpRequest) {
        Publisher<ByteBuf> requestBody = incomingHttpRequest.body().as(Publisher.class);
        long requestContentLength = HttpGatewayRequests.parsePlayRequestContentLength(incomingHttpRequest);
        RequestBuilder remoteRequestBuilder = new RequestBuilder(incomingHttpRequest.method());

        if (requestBody != null) {
            if (requestContentLength >= 0) {
                remoteRequestBuilder.setBody(requestBody, requestContentLength);
            } else {
                remoteRequestBuilder.setBody(requestBody);
            }
        }

        return new HttpGatewayRemoteRequest(
            incomingHttpRequest,
            remoteRequestBuilder
        );
    }

    public CompletableFuture<HttpGatewayRemoteResponse> executeRemoteRequest(HttpGatewayRemoteRequest remoteRequest) {
        Request request = remoteRequest.getRemoteRequest().build();
        CompletableFuture<HttpGatewayRemoteResponse> result = new CompletableFuture<>();

        asyncHttpClient.executeRequest(
            request,
            new HttpGatewayClientResponseHandler(
                result,
                request.getUrl()
            )
        );

        return result;
    }
}
