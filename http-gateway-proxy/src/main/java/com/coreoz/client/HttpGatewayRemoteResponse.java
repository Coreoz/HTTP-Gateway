package com.coreoz.client;

import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Param;
import org.reactivestreams.Publisher;

import java.util.List;

public class HttpGatewayRemoteResponse {
    private String requestUrl;

    // status code
    private int statusCode;
    // TODO replace with enum
    private boolean isTimeout;
    private boolean isError;
    private boolean hasRequestFailed;
    private String requestFailedError;

    // headers
    private List<Param> responseHeaders;
    private String contentLength;
    private String contentType;

    // response body
    private Publisher<HttpResponseBodyPart> publisher;
}
