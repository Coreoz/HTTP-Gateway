package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.*;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayClientApiKeyAuthenticator::new);
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayClientBasicAuthenticator::new);

    private static final List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs = List.of(
        KEY_AUTH,
        BASIC_AUTH
    );

    public static List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs) {
        return HttpGatewayClientAuthenticator.merge(readAuthenticators(clientsConfig, supportedAuthConfigs));
    }

    // TODO review docs

    /**
     * Read config authentication without performing merging/flattening authenticators.
     * See {@link HttpGatewayClientAuthenticator#merge(List)} for flattening authenticators
     * @param clientsConfig The client configuration configs
     * @param supportedAuthConfigs The supported authentication methods
     * @return The authenticators for each authentication method
     */
    @VisibleForTesting
    static List<HttpGatewayClientAuthenticator> readAuthenticators(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<?>> supportedAuthConfigs) {
        Map<String, HttpGatewayClientAuthConfig<?>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                availableAuthConfig -> availableAuthConfig.authConfig.getAuthType(),
                Function.identity()
            ));

        //noinspection unchecked
        return clientsConfig
            .stream()
            .map(clientConfig -> {
                // read client authentication
                ClientIdWithAuthData clientIdWithAuthData = new ClientIdWithAuthData(
                    clientConfig.getString("client-id"),
                    HttpGatewayConfigAuth.readAuthentication(
                        clientConfig,
                        authKey -> indexedAuthenticatorCreator.get(authKey).getAuthConfig().getAuthReader()
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
                (HttpGatewayClientAuthenticatorCreator<Object>) indexedAuthenticatorCreator
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
