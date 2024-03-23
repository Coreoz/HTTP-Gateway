package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthApiKey;
import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.exception.HttpGatewayValidationException;
import com.coreoz.http.services.auth.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.services.auth.HttpGatewayRemoteServicesAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceKeyAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.typesafe.config.Config;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handle remote service authentication configuration
 */
public class HttpGatewayConfigServicesAuth {
    public static final Map.Entry<String, HttpGatewayServiceAuthConfig<HttpGatewayAuthBasic>> BASIC_AUTH = makeAuthMapEntry(HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayRemoteServiceBasicAuthenticator::new));
    public static final Map.Entry<String, HttpGatewayServiceAuthConfig<HttpGatewayAuthApiKey>> KEY_AUTH = makeAuthMapEntry(HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.KEY_AUTH, HttpGatewayRemoteServiceKeyAuthenticator::new));

    private static <T> Map.Entry<String, HttpGatewayServiceAuthConfig<T>> makeAuthMapEntry(HttpGatewayServiceAuthConfig<T> authConfig) {
        return Map.entry(
            authConfig.getAuthConfig().getAuthType(),
            authConfig
        );
    }

    /**
     * Available remote services authenticators
     */
    private static final Map<String, HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs = Map.ofEntries(
        BASIC_AUTH,
        KEY_AUTH
    );

    public static Map<String, HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs() {
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

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServicesAuthenticator}
     * and enable to use custom authenticators.<br>
     * <br>
     * See {@link #readConfig(Config)} to use default available authenticators.
     */
    public static @NotNull HttpGatewayRemoteServicesAuthenticator readConfig(@NotNull Config gatewayConfig, @NotNull Map<String, HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        return new HttpGatewayRemoteServicesAuthenticator(
            HttpGatewayConfigServices
                .readRemoteServicesConfig(gatewayConfig)
                .stream()
                .map(serviceConfig -> new HttpGatewayRemoteServiceAuth(
                    serviceConfig.getString(HttpGatewayConfigServices.CONFIG_SERVICE_ID),
                    readRemoteServiceAuthentication(serviceConfig, supportedAuthConfigs)
                ))
                .filter(serviceAuth -> serviceAuth.getAuthenticator() != null)
                .collect(Collectors.toMap(
                    HttpGatewayRemoteServiceAuth::getServiceId,
                    HttpGatewayRemoteServiceAuth::getAuthenticator
                )),
            Map.of()
        );
    }

    /**
     * Read a service authenticator in a config object.<br>
     * See {@link HttpGatewayConfigAuth#readAuthentication(Config, Function)} for details
     * or <code>HttpGatewayConfigOpenApiServices</code> in the <code>config-openapi</code> module for sample usage.
     * @param objectConfig The config object containing the auth config, for example <code>{auth={type="basic", userId="abcd", password="azerty"}}</code>
     * @param indexedSupportedAuthentications The supported authentication, use {@link #supportedAuthConfigs()} for default
     * @return The service authenticator if the configuration is valid, or null if there is no auth object
     * @throws HttpGatewayValidationException if the authentication type is not present in {@code supportedAuthConfigs}
     */
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
