package com.coreoz.http.validation;

/**
 * This exception is thrown when a config validation rule fails:<br>
 * - A referenced id does not exist<br>
 * - A value is incorrect<br>
 * <br>
 * This exception is used only for config value validation,
 * if there is a wrong key used, then a {@code ConfigException} is thrown.
 */
public class HttpGatewayConfigException extends RuntimeException {
    public HttpGatewayConfigException(String message) {
        super(message);
    }
}
