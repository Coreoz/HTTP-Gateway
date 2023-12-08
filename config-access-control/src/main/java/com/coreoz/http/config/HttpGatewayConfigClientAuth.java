package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.access.control.auth.HttpGatewayClientApiKeyAuthenticator;
import com.coreoz.http.access.control.auth.HttpGatewayClientAuthenticator;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpGatewayConfigClientAuth {
    public static final HttpGatewayClientAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayClientAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayClientApiKeyAuthenticator::new);

    private final static List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs = List.of(
        KEY_AUTH
    );

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig) {
        return readAuth(clientsConfig, supportedAuthConfigs());
    }

    public static HttpGatewayClientAuthenticator readAuth(List<? extends Config> clientsConfig, List<HttpGatewayClientAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
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
        return makeAuthenticator(indexedAuthenticatorCreator, authReadConfigs);
    }

    private static HttpGatewayClientAuthenticator makeAuthenticator(
        Map<String, HttpGatewayClientAuthenticatorCreator<? extends HttpGatewayAuthObject>> indexedAuthenticatorCreator,
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs
        ) {
        return HttpGatewayClientAuthenticator.merge(authReadConfigs
            .entrySet()
            .stream()
            .map((authConfig) -> {
                //noinspection unchecked
                HttpGatewayClientAuthenticatorCreator<HttpGatewayAuthObject> creator = (HttpGatewayClientAuthenticatorCreator<HttpGatewayAuthObject>) indexedAuthenticatorCreator.get(authConfig.getKey());
                //noinspection unchecked
                return creator.createAuthenticator((List<HttpGatewayAuthObject>) authConfig.getValue());
            })
            .collect(Collectors.toList()));
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
