package com.coreoz.http.validation;

import com.coreoz.http.play.responses.HttpGatewayDownstreamError;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpGatewayValidation<T> {
    private final T value;
    private final HttpGatewayDownstreamError error;

    private HttpGatewayValidation(T value, HttpGatewayDownstreamError error) {
        this.value = value;
        this.error = error;
    }

    public interface HttpGatewayValidationFunction<T, F> {
        HttpGatewayValidation<F> validate(T input);
    }

    public static <T> HttpGatewayValidation<T> ofValue(T value) {
        return new HttpGatewayValidation<>(value, null);
    }

    public static <T> HttpGatewayValidation<T> ofError(HttpGatewayDownstreamError error) {
        return new HttpGatewayValidation<>(null, error);
    }

    public static <T> HttpGatewayValidation<T> ofError(HttpResponseStatus status, String message) {
        return ofError(new HttpGatewayDownstreamError(status, message));
    }

    public boolean isError() {
        return error != null;
    }

    public T value() {
        return value;
    }

    public HttpGatewayDownstreamError error() {
        return error;
    }

    public <F> HttpGatewayValidation<F> then(HttpGatewayValidationFunction<T, F> nextValidation) {
        if (isError()) {
            //noinspection unchecked
            return (HttpGatewayValidation<F>) this;
        }
        return nextValidation.validate(value);
    }
}
