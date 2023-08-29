package com.coreoz.http.router.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientApiKeyAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthApiKey;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final ClientConfigAuth<HttpGatewayClientAuthApiKey> KEY_AUTH = ClientConfigAuth.of("key", HttpGatewayConfigClientAuth::readAuthKey, HttpGatewayClientApiKeyAuthenticator::new);

    private final static List<ClientConfigAuth<?>> supportedAuthConfigs = List.of(
        KEY_AUTH
    );

    public static HttpGatewayAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayAuthenticator readAuth(List<? extends Config> clientsConfig, List<ClientConfigAuth<?>> supportedAuthConfigs) {
        Map<String, ClientConfigAuth<?>> indexedSupportedAuthConfigs = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                ClientConfigAuth::getAuthType,
                Function.identity()
            ));
        Map<String, List<?>> authReadConfigs = new HashMap<>();
        for (Config clientConfig : clientsConfig) {
            String clientId = clientConfig.getString("clientId");
            String authType = clientConfig.getString("type");
            ClientConfigAuth<?> authConfig = indexedSupportedAuthConfigs.get(authType);
            if (authConfig == null) {
                throw new IllegalArgumentException("Unrecognized authentication type '"+authType+"' for client '"+clientId+"'");
            }
            Object authConfigObject = authConfig.authReader.apply(clientId, clientConfig.getConfig("auth"));
            List<Object> authConfigObjects = (List<Object>) authReadConfigs.computeIfAbsent(authType, authTypeLambda -> new ArrayList<>());
            authConfigObjects.add(authConfigObject);
        }
        return makeAuthenticator(indexedSupportedAuthConfigs, authReadConfigs);
    }

    private static HttpGatewayAuthenticator makeAuthenticator(
        Map<String, ClientConfigAuth<?>> indexedSupportedAuthConfigs,
        Map<String, List<?>> authReadConfigs
        ) {
        return HttpGatewayAuthenticator.merge(authReadConfigs
            .entrySet()
            .stream()
            .map((authConfig) -> {
                ClientConfigAuth<?> clientAuthConfig = indexedSupportedAuthConfigs.get(authConfig.getKey());
                return clientAuthConfig.authenticatorCreator.apply((List) authConfig.getValue());
            })
            .collect(Collectors.toList()));
    }

    public static HttpGatewayClientAuthApiKey readAuthKey(String clientId, Config authConfig) {
        return new HttpGatewayClientAuthApiKey(clientId, authConfig.getString("value"));
    }

    public static List<ClientConfigAuth<?>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    @Value(staticConstructor = "of")
    public static class ClientConfigAuth<T> {
        String authType;
        BiFunction<String, Config, T> authReader;
        Function<List<T>, HttpGatewayAuthenticator> authenticatorCreator;
    }
}
