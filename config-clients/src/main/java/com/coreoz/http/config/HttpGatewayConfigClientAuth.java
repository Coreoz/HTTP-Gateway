package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.*;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final Map.Entry<String, HttpGatewayClientAuthConfig<HttpGatewayAuthApiKey>> KEY_AUTH = makeAuthMapEntry(
        HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayClientApiKeyAuthenticator::new)
    );
    public static final Map.Entry<String, HttpGatewayClientAuthConfig<HttpGatewayAuthBasic>> BASIC_AUTH = makeAuthMapEntry(
        HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayClientBasicAuthenticator::new)
    );

    private static <T> Map.Entry<String, HttpGatewayClientAuthConfig<T>> makeAuthMapEntry(HttpGatewayClientAuthConfig<T> authConfig) {
        return Map.entry(
            authConfig.getAuthConfig().getAuthType(),
            authConfig
        );
    }

    private static final Map<String, HttpGatewayClientAuthConfig<?>> supportedAuthConfigs = Map.ofEntries(
        KEY_AUTH,
        BASIC_AUTH
    );

    public static Map<String, HttpGatewayClientAuthConfig<?>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig, Map<String, HttpGatewayClientAuthConfig<?>> supportedAuthConfigs) {
        return HttpGatewayClientAuthenticator.merge(readAuthenticators(clientsConfig, supportedAuthConfigs));
    }

    /**
     * Read config authentication without performing merging/flattening authenticators.
     * See {@link HttpGatewayClientAuthenticator#merge(List)} for flattening authenticators
     * @param clientsConfig The client configuration configs
     * @param supportedAuthConfigs The supported authentication methods
     * @return The authenticators for each authentication method
     */
    @VisibleForTesting
    static List<HttpGatewayClientAuthenticator> readAuthenticators(List<? extends Config> clientsConfig, Map<String, HttpGatewayClientAuthConfig<?>> supportedAuthConfigs) {
        //noinspection unchecked
        return clientsConfig
            .stream()
            .map(clientConfig -> {
                // read client authentication
                ClientIdWithAuthData clientIdWithAuthData = new ClientIdWithAuthData(
                    clientConfig.getString("client-id"),
                    HttpGatewayConfigAuth.readAuthentication(
                        clientConfig,
                        authKey -> supportedAuthConfigs.get(authKey).getAuthConfig().getAuthReader()
                    )
                );
                if (clientIdWithAuthData.authData() == null) {
                    throw new RuntimeException("Mandatory authentication is missing for client " + clientIdWithAuthData.clientId());
                }
                return clientIdWithAuthData;
            })
            .collect(Collectors.groupingBy(
                // group client identifications by auth method (key, basic, etc.)
                clientIdWithAuthData -> clientIdWithAuthData.authData().getAuthType(),
                Collectors.mapping(
                    clientIdWithAuthData -> new HttpGatewayClientAuth<Object>(
                        clientIdWithAuthData.clientId(),
                        clientIdWithAuthData.authData().getAuthObject()
                    ),
                    Collectors.toList()
                )
            ))
            .entrySet()
            .stream()
            .map(clientAuthGroup -> (
                // build the authenticator
                (HttpGatewayClientAuthenticatorCreator<Object>) supportedAuthConfigs
                    .get(clientAuthGroup.getKey())
                    .getAuthenticatorCreator()
                )
                .createAuthenticator(clientAuthGroup.getValue())
            )
            .toList();
    }

    /**
     * A client authenticator configuration
     * @param <T> The type of the object that represents the authentication. See <code>HttpGatewayAuthBasic</code> for an example
     */
    @Value(staticConstructor = "of")
    public static class HttpGatewayClientAuthConfig<T> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        HttpGatewayClientAuthenticatorCreator<T> authenticatorCreator;
    }

    record ClientIdWithAuthData(String clientId, HttpGatewayConfigAuth.HttpGatewayAuth<?> authData) {}

    /**
     * Function that creates an {@link HttpGatewayClientAuthenticator} from a list of authentication objects.
     * See {@link HttpGatewayClientApiKeyAuthenticator#HttpGatewayClientApiKeyAuthenticator(List)} for an example
     * @param <T> See {@link HttpGatewayClientAuthConfig}
     */
    @FunctionalInterface
    public interface HttpGatewayClientAuthenticatorCreator<T> {
        HttpGatewayClientAuthenticator createAuthenticator(List<HttpGatewayClientAuth<T>> authObjects);
    }
}
