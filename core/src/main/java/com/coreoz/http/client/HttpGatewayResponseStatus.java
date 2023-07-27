package com.coreoz.http.client;

public enum HttpGatewayResponseStatus {
	OK,
    /**
     * The remote server didn't respond during the allowed delay
     */
    REMOTE_TIMEOUT,
    /**
     * The remote server responded with a 4xx error
     */
    REMOTE_CLIENT_ERROR,
    /**
     * The remote server responded with a 5xx error
     */
    REMOTE_SERVER_ERROR,
    /**
     * The HTTP Gateway could not connect to the remote server (wrong host, SSL error),
     * or the HTTP Gateway produced an error transforming the response
     */
    HTTP_GATEWAY_ERROR,
}
