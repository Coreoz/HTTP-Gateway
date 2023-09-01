package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigAuth {
    public static final HttpGatewayAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayAuthConfig.of("key", HttpGatewayConfigAuth::readAuthKey);

    public static Map<String, List<?>> readAuth(
        String configObjectId, List<? extends Config> objectsConfig, List<HttpGatewayAuthConfig<?>> supportedAuthConfigs
    ) {
        Map<String, HttpGatewayAuthConfig<?>> indexedSupportedAuthConfigs = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayAuthConfig::getAuthType,
                Function.identity()
            ));
        Map<String, List<?>> authReadConfigs = new HashMap<>();
        for (Config objectConfig : objectsConfig) {
            String objectId = objectConfig.getString(configObjectId);
            Config baseAuthConfig = objectConfig.getConfig("auth");
            String authType = baseAuthConfig.getString("type");
            HttpGatewayAuthConfig<?> authConfig = indexedSupportedAuthConfigs.get(authType);
            if (authConfig == null) {
                throw new IllegalArgumentException("Unrecognized authentication type '"+authType+"' for "+configObjectId+"="+objectId);
            }
            Object authConfigObject = authConfig.authReader.apply(objectId, baseAuthConfig);
            List<Object> authConfigObjects = (List<Object>) authReadConfigs.computeIfAbsent(authType, authTypeLambda -> new ArrayList<>());
            authConfigObjects.add(authConfigObject);
        }
        return authReadConfigs;
    }

    private static HttpGatewayAuthApiKey readAuthKey(String objectId, Config authConfig) {
        return new HttpGatewayAuthApiKey(objectId, authConfig.getString("value"));
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayAuthConfig<T> {
        String authType;
        BiFunction<String, Config, T> authReader;
    }
}
