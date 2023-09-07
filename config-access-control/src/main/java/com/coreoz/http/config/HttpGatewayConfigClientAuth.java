package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientApiKeyAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayClientApiKeyAuthenticator::new);

    private final static List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs = List.of(
        KEY_AUTH
    );

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs) {
        Map<String, List<?>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            "client-id",
            clientsConfig,
            supportedAuthConfigs.stream().map(HttpGatewayClientAuthConfig::getAuthConfig).collect(Collectors.toList())
        );

        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, Function<List<?>, HttpGatewayClientAuthenticator>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                availableAuthConfig -> availableAuthConfig.authConfig.getAuthType(),
                availableAuthConfig -> (Function) availableAuthConfig.getAuthenticatorCreator()
            ));
        return makeAuthenticator(indexedAuthenticatorCreator, authReadConfigs);
    }

    private static HttpGatewayClientAuthenticator makeAuthenticator(
        Map<String, Function<List<?>, HttpGatewayClientAuthenticator>> indexedAuthenticatorCreator,
        Map<String, List<?>> authReadConfigs
        ) {
        return HttpGatewayClientAuthenticator.merge(authReadConfigs
            .entrySet()
            .stream()
            .map((authConfig) -> {
                Function<List<?>, HttpGatewayClientAuthenticator> clientAuthenticatorCreator = indexedAuthenticatorCreator.get(authConfig.getKey());
                return clientAuthenticatorCreator.apply(authConfig.getValue());
            })
            .collect(Collectors.toList()));
    }

    public static List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayClientAuthConfig<T> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        Function<List<T>, HttpGatewayClientAuthenticator> authenticatorCreator;
    }
}
