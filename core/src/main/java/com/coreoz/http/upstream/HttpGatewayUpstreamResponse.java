package com.coreoz.http.upstream;

import lombok.Getter;
import lombok.Setter;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class HttpGatewayUpstreamResponse {
    private @NotNull String requestUrl;

    // status code
    private int statusCode;
    private @NotNull HttpGatewayResponseStatus responseStatus = HttpGatewayResponseStatus.HTTP_GATEWAY_ERROR;
    private @Nullable Throwable gatewayError;

    // headers
    /**
     * Response header without content-length and content-type
     */
    private @NotNull List<Param> responseHeaders = new ArrayList<>();
    private @Nullable String contentLength;
    private @Nullable String contentType;

    // response body
    private @Nullable Publisher<HttpResponseBodyPart> publisher;

    public HttpGatewayUpstreamResponse(@NotNull String requestUrl) {
        this.requestUrl = requestUrl;
    }
}
