package com.coreoz.http.validation;

import com.coreoz.http.play.responses.HttpGatewayDownstreamError;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Represents an incoming downstream validation result.
 * For example, identifying a client can use this validation result:
 * if everything goes well, the {@link #value()} field will contain the identified client object,
 * if there is a validation issue, the {@link #error()} value will contain the {@link HttpGatewayDownstreamError} object.<br>
 * <br>
 * Validations can be combined directly, for example, if the method {@code validateClient()} returns
 * a {@code HttpGatewayValidation<Client>} object, it is possible to use like this;
 * {@code validateClient(downstreamRequest).then(client -> HttpGatewayValidation.of(router.hasAccess(client)}.
 * This way, all the validations are linked to each other, and if one fails, the other validations will not be executed.
 * @param <T> The success result type of the validation
 */
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

    /**
     * Create a success validation result
     * @param value The success value
     */
    public static <T> HttpGatewayValidation<T> ofValue(T value) {
        return new HttpGatewayValidation<>(value, null);
    }

    /**
     * Create an error validation result.<br>
     * <br>
     * When an error validation is created, the {@link #then(HttpGatewayValidationFunction)} validation will not get
     * called if it used.
     * @param error The {@link HttpGatewayDownstreamError} error
     */
    public static <T> HttpGatewayValidation<T> ofError(HttpGatewayDownstreamError error) {
        return new HttpGatewayValidation<>(null, error);
    }

    /**
     * See {@link #ofError(HttpGatewayDownstreamError)}
     * @param status The error status
     * @param message The error message
     */
    public static <T> HttpGatewayValidation<T> ofError(HttpResponseStatus status, String message) {
        return ofError(new HttpGatewayDownstreamError(status, message));
    }

    /**
     * Returns true if the validation object contains an error.
     * Else true is returned.
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Return the validation value if it exists, else null
     */
    public T value() {
        return value;
    }

    /**
     * Return the validation error if it exists, else null
     */
    public HttpGatewayDownstreamError error() {
        return error;
    }

    /**
     * Execute an other validation only if the current validation already succeed.
     * @param nextValidation The function that will be called only if the current validation is a success. If the current validation is an error, this {@code nextValidation} will not be called
     * @return The result of the execution of the {@code nextValidation} parameter if it is executed. If the current validation is already an error, then this validation object is returned
     */
    public <F> HttpGatewayValidation<F> then(HttpGatewayValidationFunction<T, F> nextValidation) {
        if (isError()) {
            //noinspection unchecked
            return (HttpGatewayValidation<F>) this;
        }
        return nextValidation.validate(value);
    }
}
