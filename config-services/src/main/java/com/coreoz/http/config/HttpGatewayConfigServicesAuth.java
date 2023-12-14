package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle remote service authentication configuration
 */
public class HttpGatewayConfigServicesAuth {
    public static final HttpGatewayServiceAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayRemoteServiceBasicAuthenticator::new);

    /**
     * Available remote services authenticators
     */
    private final static List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs = List.of(
        BASIC_AUTH
    );

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServiceAuthenticator}
     * using {@link #supportedAuthConfigs}
     */
    public static HttpGatewayRemoteServiceAuthenticator readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServiceAuthenticator}
     * using {@link #supportedAuthConfigs}
     */
    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, supportedAuthConfigs);
    }

    /**
     * Read remote services authentication config to create a {@link HttpGatewayRemoteServiceAuthenticator}
     * and enable to use custom authenticators.<br>
     * <br>
     * See {@link #readConfig(Config)} to use default available authenticators.
     */
    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig, List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            HttpGatewayConfigServices.CONFIG_SERVICE_ID,
            HttpGatewayConfigServices.readRemoteServicesConfig(gatewayConfig),
            supportedAuthConfigs.stream().map(HttpGatewayServiceAuthConfig::getAuthConfig).collect(Collectors.toList())
        );
        List<HttpGatewayRemoteServiceAuth> serviceAuthentications = createServiceAuthentications(supportedAuthConfigs, authReadConfigs);
        return HttpGatewayRemoteServiceAuthenticator.fromRemoteClientAuthentications(serviceAuthentications);
    }

    @VisibleForTesting
    static List<HttpGatewayRemoteServiceAuth> createServiceAuthentications(List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs, Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs) {
        Map<String, HttpGatewayUpstreamAuthenticatorCreator<? extends HttpGatewayAuthObject>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                authConfig -> authConfig.getAuthConfig().getAuthType(),
                HttpGatewayServiceAuthConfig::getAuthenticatorCreator
            ));

        //noinspection unchecked
        return authReadConfigs
            .entrySet()
            .stream()
            .flatMap(serviceAuthByType -> serviceAuthByType.getValue().stream().map(serviceAuth -> new HttpGatewayRemoteServiceAuth(
                serviceAuth.getObjectId(),
                (
                    (HttpGatewayUpstreamAuthenticatorCreator<HttpGatewayAuthObject>) indexedAuthenticatorCreator.get(serviceAuthByType.getKey())
                )
                    .createAuthenticator(serviceAuth)
            )))
            .collect(Collectors.toList());
    }

    /**
     * A service authenticator configuration
     * @param <T> The type of the object that represents the authentication. See <code>HttpGatewayAuthBasic</code> for an example
     */
    @Value(staticConstructor = "of")
    public static class HttpGatewayServiceAuthConfig<T extends HttpGatewayAuthObject> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        HttpGatewayUpstreamAuthenticatorCreator<T> authenticatorCreator;
    }

    /**
     * Function that creates an {@link HttpGatewayUpstreamAuthenticator} from an authentication object
     * @param <T> See {@link HttpGatewayServiceAuthConfig}
     */
    @FunctionalInterface
    public interface HttpGatewayUpstreamAuthenticatorCreator<T extends HttpGatewayAuthObject> {
        HttpGatewayUpstreamAuthenticator createAuthenticator(T authObject);
    }
}
