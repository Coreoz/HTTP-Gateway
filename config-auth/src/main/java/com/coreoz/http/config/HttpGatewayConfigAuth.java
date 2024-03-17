package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.exception.HttpGatewayValidationException;
import com.typesafe.config.Config;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Read authentication information in a list of objects in a config file.
 * Objects can be services, clients, or anything else.
 */
public class HttpGatewayConfigAuth {
    public static final HttpGatewayAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayAuthConfig.of("key", HttpGatewayConfigAuth::readAuthKey);
    public static final HttpGatewayAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayAuthConfig.of("basic", HttpGatewayConfigAuth::readBasicAuth);

    public static final String CONFIG_AUTH_NAME = "auth";

    // TODO review docs

    /**
     * Read authentication from a list of objects
     * @param authReaderFetcher The list of supported authentication methods
     * @return A {@code Map} containing auth object indexed by authentication method/type.
     * So the {@code Map} key is the authentication method/type, and the authentication value is the {@code List}
     * of read objects that matche this authentication. See {@link HttpGatewayAuthConfig} for how object are read.
     * @throws HttpGatewayValidationException if the authentication type is not present in {@code supportedAuthConfigs}
     */
    public static @Nullable HttpGatewayAuth<?> readAuthentication(
        @NotNull Config objectConfig,
        Function<@NotNull String, @Nullable HttpGatewayAuthReader<?>> authReaderFetcher
    ) {
        if (!objectConfig.hasPath(CONFIG_AUTH_NAME)) {
            return null;
        }
        Config baseAuthConfig = objectConfig.getConfig(CONFIG_AUTH_NAME);
        String authType = baseAuthConfig.getString("type");
        HttpGatewayAuthReader<?> authReader = authReaderFetcher.apply(authType);
        if (authReader == null) {
            throw new HttpGatewayValidationException("Unrecognized authentication type '" + authType + "' for " + objectConfig);
        }
        return HttpGatewayAuth.of(
            authType,
            authReader.readAuthConfig(baseAuthConfig)
        );
    }

    private static HttpGatewayAuthApiKey readAuthKey(Config authConfig) {
        return new HttpGatewayAuthApiKey(authConfig.getString("value"));
    }

    private static HttpGatewayAuthBasic readBasicAuth(Config authConfig) {
        return new HttpGatewayAuthBasic(
            authConfig.getString("userId"),
            authConfig.getString("password")
        );
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayAuthConfig<T> {
        String authType;
        HttpGatewayAuthReader<T> authReader;
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayAuth<T> {
        String authType;
        T authObject;
    }

    /**
     * The function that will read authentication data in a config object.
     * @param <T> The authentication object type that will contain all the necessary data to perform the authentication
     */
    @FunctionalInterface
    public interface HttpGatewayAuthReader<T> {
        /**
         * @param authConfig The authentication {@link Config} object for the current objectId. For a key auth, the object will be <code>{type = "key", value = "sample-api-kay"}</code>
         * @return The authentication object that will contain all the necessary data to perform the authentication
         */
        T readAuthConfig(Config authConfig);
    }
}
