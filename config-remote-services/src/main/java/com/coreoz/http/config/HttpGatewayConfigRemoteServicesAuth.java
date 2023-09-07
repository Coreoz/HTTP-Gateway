package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServicesAuth {
    public static final HttpGatewayServiceAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayRemoteServiceBasicAuthenticator::new);

    private final static List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs = List.of(
        BASIC_AUTH
    );

    public static HttpGatewayRemoteServiceAuthenticator readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, supportedAuthConfigs);
    }

    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig, List<HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        Map<String, List<?>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            HttpGatewayConfigRemoteServices.CONFIG_SERVICE_ID,
            HttpGatewayConfigRemoteServices.readRemoteServicesConfig(gatewayConfig),
            supportedAuthConfigs.stream().map(HttpGatewayServiceAuthConfig::getAuthConfig).collect(Collectors.toList())
        );
        Map<String, Function<?, HttpGatewayUpstreamAuthenticator>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                authConfig -> authConfig.getAuthConfig().getAuthType(),
                HttpGatewayServiceAuthConfig::getAuthenticatorCreator
            ));

        // TODO split and try to unit test this
        return HttpGatewayRemoteServiceAuthenticator.fromRemoteClientAuthentications(
            authReadConfigs
                .entrySet()
                .stream()
                .flatMap(serviceAuthByType -> serviceAuthByType.getValue().stream().map(serviceAuth -> new HttpGatewayRemoteServiceAuth(
                    serviceAuthByType.getKey(),
                    createAuthenticator(serviceAuthByType.getKey(), serviceAuth, indexedAuthenticatorCreator)
                )))
                .collect(Collectors.toList())
        );
    }

    private static HttpGatewayUpstreamAuthenticator createAuthenticator(
        String authType,
        Object serviceAuth,
        Map<String, Function<?, HttpGatewayUpstreamAuthenticator>> indexedAuthenticatorCreator
    ) {
        //noinspection unchecked,rawtypes
        return (HttpGatewayUpstreamAuthenticator) ((Function) indexedAuthenticatorCreator.get(authType)).apply(serviceAuth);
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayServiceAuthConfig<T> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        Function<T, HttpGatewayUpstreamAuthenticator> authenticatorCreator;
    }
}
