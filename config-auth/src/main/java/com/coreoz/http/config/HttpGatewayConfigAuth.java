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

    /**
     * Read authentication an object from a config.<br>
     * <br>
     * A config that contains an authentication must look like that: <code>{auth={type="basic", userId="abcd", password="azerty"}}</code><br>
     * In this example, there are:<br>
     * - The <code>auth</code> object that will contain all configurations related to authentication<br>
     * - The authentication <code>type</code> that specifies what authentication is configured: basic, key, etc.<br>
     * - The authentication type arguments, here the <code>userId</code> and the <code>password</code> for the basic auth
     * @param objectConfig The config that contains the <code>auth</code> config
     * @param authReaderFetcher The function that enables to retrieve an authentication config reader from an authentication <code>type</code>.
     *                          It is generally provided by a Map object. See HttpGatewayConfigClientAuth or HttpGatewayConfigServicesAuth
     *                          for sample usages. This classes should generally be used to read client or services authentication.
     * @return The auth object created by the {@link HttpGatewayAuthReader}. If the <code>auth</code> config is missing,
     * then null will be returned.
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

    /**
     * An authentication reader configuration: the authentication type and the corresponding {@link HttpGatewayAuthReader}
     * @param <T> The type of the object data read by the {@link HttpGatewayAuthReader}
     */
    @Value(staticConstructor = "of")
    public static class HttpGatewayAuthConfig<T> {
        String authType;
        HttpGatewayAuthReader<T> authReader;
    }

    /**
     * An authentication type with the data that has been read by a {@link HttpGatewayAuthReader}
     * @param <T> The type of the data object read
     */
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
