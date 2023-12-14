package com.coreoz.http.play;

import com.google.common.net.HttpHeaders;
import org.asynchttpclient.Param;
import org.asynchttpclient.RequestBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpGatewayDownstreamRequests {
    public static void copyHeader(Http.Request incomingRequest, RequestBuilder remoteRequest, String httpHeader) {
        incomingRequest.header(httpHeader).ifPresent(headerValue -> remoteRequest.addHeader(httpHeader, headerValue));
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

    public static List<Param> convertQueryParamsToParams(Map<String, String[]> queryParams) {
        return queryParams
            .entrySet()
            .stream()
            .flatMap(entry ->
                Arrays
                    .stream(entry.getValue())
                    .map(value -> new Param(entry.getKey(), value))
            )
            .collect(Collectors.toList());
    }
}
