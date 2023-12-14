package com.coreoz.http.exception;

/**
 * This exception is thrown when an integrity validation rule fails:<br>
 * - A referenced id does not exist<br>
 * - A value is incorrect<br>
 * <br>
 * This exception is used only for integrity validation,
 * so for example, within the config modules, if a wrong key is used,
 * then a {@code ConfigException} is thrown.
 */
public class HttpGatewayValidationException extends RuntimeException {
    public HttpGatewayValidationException(String message) {
        super(message);
    }
}
