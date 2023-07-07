package com.coreoz.play;

import com.google.common.net.HttpHeaders;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

public class HttpGatewayRequests {
    public static void copyHeader(Http.Request incomingRequest, RequestBuilder remoteRequest, String httpHeader) {
        remoteRequest.addHeader(httpHeader, incomingRequest.header(httpHeader).orElse(null));
    }

    /**
     * Forward basic headers from the incoming request toward the remote request:
     * <ul>
     *     <li>{@link HttpHeaders.CONTENT_TYPE}</li>
     *     <li>{@link HttpHeaders.ACCEPT}</li>
     *     <li>{@link HttpHeaders.ACCEPT_CHARSET}</li>
     *     <li>{@link HttpHeaders.ACCEPT_ENCODING}</li>
     *     <li>{@link HttpHeaders.ACCEPT_LANGUAGE}</li>
     *     <li>{@link HttpHeaders.COOKIE}</li>
     * </ul>
     * @param remoteRequest The remote request
     */
    public static void copyBasicHeaders(Http.Request incomingRequest, RequestBuilder remoteRequest) {
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.CONTENT_TYPE);
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.ACCEPT);
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.ACCEPT_CHARSET);
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.ACCEPT_ENCODING);
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.ACCEPT_LANGUAGE);
        copyHeader(incomingRequest, remoteRequest, HttpHeaders.COOKIE);
    }

    /**
     * Parse the content length header.
     * If the header is absent or if an error occurred during the parsing, -1 is returned
     */
    public static long parsePlayRequestContentLength(Http.Request request) {
        return request
            .header(HttpHeaders.CONTENT_LENGTH)
            .map(contentLength -> {
                try {
                    return Long.parseLong(contentLength);
                } catch (Exception e) {
                    return -1L;
                }
            })
            .orElse(-1L);
    }
}
