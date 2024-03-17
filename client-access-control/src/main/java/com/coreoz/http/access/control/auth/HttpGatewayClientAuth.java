package com.coreoz.http.access.control.auth;

/**
 * Contain an authentication object for a clientId. This object will be used to create a client authenticator
 * @param clientId The identifier of a client
 * @param authObject The authentication data (i.e. the username/password for a base64 auth)
 * @param <T> The Type of the {@link #authObject}
 */
public record HttpGatewayClientAuth<T>(String clientId, T authObject) {
}
