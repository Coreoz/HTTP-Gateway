package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.services.auth.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceKeyAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.typesafe.config.Config;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handle remote service authentication configuration
 */
public class HttpGatewayConfigServicesAuth {
    public static final HttpGatewayServiceAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayRemoteServiceBasicAuthenticator::new);
    public static final HttpGatewayServiceAuthConfig<HttpGatewayAuthApiKey> KEY_AUTH = HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayRemoteServiceKeyAuthenticator::new);

    /**
     * Available remote services authenticators
     */
    private static final List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs = List.of(
        BASIC_AUTH,
        KEY_AUTH
    );

    public static List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs() {
        return supportedAuthConfigs;
    }

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServicesAuthenticator}
     * using {@link #supportedAuthConfigs}
     */
    public static HttpGatewayRemoteServicesAuthenticator readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServicesAuthenticator}
     * using {@link #supportedAuthConfigs}
     */
    public static HttpGatewayRemoteServicesAuthenticator readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, supportedAuthConfigs);
    }

    // TODO review docs
    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServicesAuthenticator}
     * and enable to use custom authenticators.<br>
     * <br>
     * See {@link #readConfig(Config)} to use default available authenticators.
     */
    public static @NotNull HttpGatewayRemoteServicesAuthenticator readConfig(@NotNull Config gatewayConfig, @NotNull List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        Map<String, HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs = indexAuthenticationConfigs(supportedAuthConfigs);
        return new HttpGatewayRemoteServicesAuthenticator(
            HttpGatewayConfigServices
                .readRemoteServicesConfig(gatewayConfig)
                .stream()
                .map(serviceConfig -> new HttpGatewayRemoteServiceAuth(
                    serviceConfig.getString(HttpGatewayConfigServices.CONFIG_SERVICE_ID),
                    readRemoteServiceAuthentication(serviceConfig, indexedAuthConfigs)
                ))
                .filter(serviceAuth -> serviceAuth.getAuthenticator() != null)
                .collect(Collectors.toMap(
                    HttpGatewayRemoteServiceAuth::getServiceId,
                    HttpGatewayRemoteServiceAuth::getAuthenticator
                )),
            Map.of()
        );
    }

    public static @NotNull Map<String, HttpGatewayServiceAuthConfig<?>> indexAuthenticationConfigs(@NotNull List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        return supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                authConfig -> authConfig.getAuthConfig().getAuthType(),
                Function.identity()
            ));
    }

    public static @Nullable HttpGatewayUpstreamAuthenticator readRemoteServiceAuthentication(
        @NotNull Config objectConfig,
        @NotNull Map<String, HttpGatewayServiceAuthConfig<?>> indexedSupportedAuthentications
    ) {
        HttpGatewayConfigAuth.HttpGatewayAuth<?> authenticationConfig = HttpGatewayConfigAuth.readAuthentication(
            objectConfig,
            authKey -> indexedSupportedAuthentications.get(authKey).getAuthConfig().getAuthReader()
        );
        if (authenticationConfig != null) {
            //noinspection unchecked
            HttpGatewayUpstreamAuthenticatorCreator<Object> authenticatorCreator = (HttpGatewayUpstreamAuthenticatorCreator<Object>) indexedSupportedAuthentications
                .get(authenticationConfig.getAuthType())
                .authenticatorCreator;
            return authenticatorCreator.createAuthenticator(authenticationConfig.getAuthObject());
        }
        return null;
    }

    /**
     * A service authenticator configuration
     * @param <T> The type of the object that represents the authentication. See <code>HttpGatewayAuthBasic</code> for an example
     */
    @Value(staticConstructor = "of")
    public static class HttpGatewayServiceAuthConfig<T> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        HttpGatewayUpstreamAuthenticatorCreator<T> authenticatorCreator;
    }

    /**
     * Function that creates an {@link HttpGatewayUpstreamAuthenticator} from an authentication object
     * @param <T> See {@link HttpGatewayServiceAuthConfig}
     */
    @FunctionalInterface
    public interface HttpGatewayUpstreamAuthenticatorCreator<T> {
        HttpGatewayUpstreamAuthenticator createAuthenticator(T authObject);
    }
}
