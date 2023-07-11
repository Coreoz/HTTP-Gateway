package com.coreoz.client;

import lombok.Getter;
import lombok.Setter;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Param;
import org.reactivestreams.Publisher;

import java.util.List;

@Setter
@Getter
public class HttpGatewayRemoteResponse {
    private String requestUrl;

    // status code
    private int statusCode;
    private HttpGatewayResponseStatus responseStatus;
    private Throwable gatewayError;

    // headers
    /**
     * Response header without content-length and content-type
     */
    private List<Param> responseHeaders;
    private String contentLength;
    private String contentType;

    // response body
    private Publisher<HttpResponseBodyPart> publisher;
}
