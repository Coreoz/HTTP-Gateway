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
import java.util.function.BiFunction;
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
                HttpGatewayAuthObject authConfigObject = authConfig.authReader.apply(objectId, baseAuthConfig);
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
    public static class HttpGatewayAuthConfig<T> {
        String authType;
        BiFunction<String, Config, T> authReader;
    }
}
