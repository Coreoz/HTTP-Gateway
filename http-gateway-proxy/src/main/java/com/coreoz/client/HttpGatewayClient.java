package com.coreoz.client;

import com.google.common.net.HttpHeaders;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
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
     * Create a new AHC request from an incoming HTTP Gateway request by forwarding basic headers:
     * <ul>
     *     <li>{@link HttpHeaders.CONTENT_TYPE}</li>
     *     <li>{@link HttpHeaders.ACCEPT}</li>
     *     <li>{@link HttpHeaders.ACCEPT_CHARSET}</li>
     *     <li>{@link HttpHeaders.ACCEPT_ENCODING}</li>
     *     <li>{@link HttpHeaders.ACCEPT_LANGUAGE}</li>
     *     <li>{@link HttpHeaders.COOKIE}</li>
     * </ul>
     * @param playHttprequest
     * @return
     */
    public HttpGatewayRemoteRequest prepareRequest(Http.Request playHttprequest) {
        // TODO
        return new HttpGatewayRemoteRequest();
    }

    public CompletableFuture<HttpGatewayRemoteResponse> executeRemoteRequest(HttpGatewayRemoteRequest remoteRequest) {
        // TODO
        return null;
    }
}
