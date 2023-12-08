package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.validation.HttpGatewayConfigException;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read authentication information in a list of objects in a config file.
 * Objects can be services, clients, or anything else.
 */
public class HttpGatewayConfigAuth {
    public static final HttpGatewayAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayAuthConfig.of("key", HttpGatewayConfigAuth::readAuthKey);
    public static final HttpGatewayAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayAuthConfig.of("basic", HttpGatewayConfigAuth::readBasicAuth);

    /**
     * Read authentication from a list of objects
     * @param configObjectId The config key that identify the ID of an object item
     * @param objectsConfig The list of configs, each config item represents an object
     * @param supportedAuthConfigs The list of supported authentication methods
     * @return A {@code Map} containing auth object indexed by authentication method/type.
     * So the {@code Map} key is the authentication method/type, and the authentication value is the {@code List}
     * of read objects that matche this authentication. See {@link HttpGatewayAuthConfig} for how object are read.
     * @throws HttpGatewayConfigException if the authentication type is not present in {@code supportedAuthConfigs}
     * @throws ConfigException if a missing authentication key is absent in the objects or if the objectId key is missing
     */
    public static Map<String, List<? extends HttpGatewayAuthObject>> readAuth(
        String configObjectId, List<? extends Config> objectsConfig, List<HttpGatewayAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs
    ) {
        Map<String, HttpGatewayAuthConfig<? extends HttpGatewayAuthObject>> indexedSupportedAuthConfigs = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayAuthConfig::getAuthType,
                Function.identity()
            ));
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs = new HashMap<>();
        for (Config objectConfig : objectsConfig) {
            String objectId = objectConfig.getString(configObjectId);
            if (objectConfig.hasPath("auth")) {
                Config baseAuthConfig = objectConfig.getConfig("auth");
                String authType = baseAuthConfig.getString("type");
                HttpGatewayAuthConfig<? extends HttpGatewayAuthObject> authConfig = indexedSupportedAuthConfigs.get(authType);
                if (authConfig == null) {
                    throw new HttpGatewayConfigException("Unrecognized authentication type '" + authType + "' for " + configObjectId + "=" + objectId);
                }
                HttpGatewayAuthObject authConfigObject = authConfig.authReader.readAuthConfig(objectId, baseAuthConfig);
                @SuppressWarnings("unchecked")
                List<HttpGatewayAuthObject> authConfigObjects = (List<HttpGatewayAuthObject>) authReadConfigs.computeIfAbsent(authType, authTypeLambda -> new ArrayList<>());
                authConfigObjects.add(authConfigObject);
            }
        }
        return authReadConfigs;
    }

    private static HttpGatewayAuthApiKey readAuthKey(String objectId, Config authConfig) {
        return new HttpGatewayAuthApiKey(objectId, authConfig.getString("value"));
    }

    private static HttpGatewayAuthBasic readBasicAuth(String objectId, Config authConfig) {
        return new HttpGatewayAuthBasic(
            objectId,
            authConfig.getString("userId"),
            authConfig.getString("password")
        );
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayAuthConfig<T extends HttpGatewayAuthObject> {
        String authType;
        HttpGatewayAuthReader<T> authReader;
    }

    /**
     * The function that will read authentication data in a config object.
     * @param <T> The authentication object type that will contain all the necessary data to perform the authentication
     */
    @FunctionalInterface
    public interface HttpGatewayAuthReader<T extends HttpGatewayAuthObject> {
        /**
         * @param objectId The referenced object for the authentication. It can be a clientId or a serviceId. It will be used by <code>HttpGatewayRemoteServiceAuthenticator</code> and <code>HttpGatewayClientAuthenticator</code> to either guess which authenticator to use (for a service), or to either know which client has been authenticated
         * @param authConfig The authentication {@link Config} object for the current objectId. For a key auth, the object will be <code>{type = "key", value = "sample-api-kay"}</code>
         * @return The authentication object that will contain all the necessary data to perform the authentication
         */
        T readAuthConfig(String objectId, Config authConfig);
    }
}
