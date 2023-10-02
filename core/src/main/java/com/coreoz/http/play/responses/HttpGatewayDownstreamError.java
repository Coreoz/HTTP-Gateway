package com.coreoz.http.play.responses;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Value;

/**
 * Represents an error that will be returned to the downstream client
 */
@Value
public class HttpGatewayDownstreamError {
    /**
     * The HTTP status code that will be returned to the client
     */
    HttpResponseStatus status;
    /**
     * The JSON message that will be returned in the error response body
     */
    String message;
}
