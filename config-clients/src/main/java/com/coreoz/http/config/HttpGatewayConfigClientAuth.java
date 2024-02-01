package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.*;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayClientApiKeyAuthenticator::new);
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayClientBasicAuthenticator::new);

    private static final List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs = List.of(
        KEY_AUTH,
        BASIC_AUTH
    );

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
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
    static List<HttpGatewayClientAuthenticator> readAuthenticators(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            "client-id",
            clientsConfig,
            supportedAuthConfigs.stream().map(HttpGatewayClientAuthConfig::getAuthConfig).collect(Collectors.toList())
        );

        Map<String, HttpGatewayClientAuthenticatorCreator<? extends HttpGatewayAuthObject>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                availableAuthConfig -> availableAuthConfig.authConfig.getAuthType(),
                HttpGatewayClientAuthConfig::getAuthenticatorCreator
            ));
        return makeAuthenticators(authReadConfigs, indexedAuthenticatorCreator);
    }

    /**
     * Create authenticator objects from using authentication values read from the client config, and the indexed authenticator creators
     * @param authReadConfigs Authentication values read from the client config
     * @param indexedAuthenticatorCreator The indexed authenticator creators
     * @return The authenticators for each authentication method
     */
    private static List<HttpGatewayClientAuthenticator> makeAuthenticators(
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs,
        Map<String, HttpGatewayClientAuthenticatorCreator<? extends HttpGatewayAuthObject>> indexedAuthenticatorCreator
        ) {
        return authReadConfigs
            .entrySet()
            .stream()
            .map(authConfig -> {
                //noinspection unchecked
                HttpGatewayClientAuthenticatorCreator<HttpGatewayAuthObject> creator = (HttpGatewayClientAuthenticatorCreator<HttpGatewayAuthObject>) indexedAuthenticatorCreator.get(authConfig.getKey());
                //noinspection unchecked
                return creator.createAuthenticator((List<HttpGatewayAuthObject>) authConfig.getValue());
            })
            .toList();
    }

    public static List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    /**
     * A client authenticator configuration
     * @param <T> The type of the object that represents the authentication. See <code>HttpGatewayAuthBasic</code> for an example
     */
    @Value(staticConstructor = "of")
    public static class HttpGatewayClientAuthConfig<T extends HttpGatewayAuthObject> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        HttpGatewayClientAuthenticatorCreator<T> authenticatorCreator;
    }

    /**
     * Function that creates an {@link HttpGatewayClientAuthenticator} from a list of authentication objects.
     * See {@link HttpGatewayClientApiKeyAuthenticator#HttpGatewayClientApiKeyAuthenticator(List)} for an example
     * @param <T> See {@link HttpGatewayClientAuthConfig}
     */
    @FunctionalInterface
    public interface HttpGatewayClientAuthenticatorCreator<T extends HttpGatewayAuthObject> {
        HttpGatewayClientAuthenticator createAuthenticator(List<T> authObjects);
    }
}
