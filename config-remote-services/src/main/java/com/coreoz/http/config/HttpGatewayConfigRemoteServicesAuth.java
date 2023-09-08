package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthBasic;
import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceAuthenticator;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpGatewayConfigRemoteServicesAuth {
    public static final HttpGatewayServiceAuthConfig<HttpGatewayAuthBasic> BASIC_AUTH = HttpGatewayServiceAuthConfig.of(HttpGatewayConfigAuth.BASIC_AUTH, HttpGatewayRemoteServiceBasicAuthenticator::new);

    private final static List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs = List.of(
        BASIC_AUTH
    );

    public static HttpGatewayRemoteServiceAuthenticator readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, supportedAuthConfigs);
    }

    public static HttpGatewayRemoteServiceAuthenticator readConfig(Config gatewayConfig, List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            HttpGatewayConfigRemoteServices.CONFIG_SERVICE_ID,
            HttpGatewayConfigRemoteServices.readRemoteServicesConfig(gatewayConfig),
            supportedAuthConfigs.stream().map(HttpGatewayServiceAuthConfig::getAuthConfig).collect(Collectors.toList())
        );
        List<HttpGatewayRemoteServiceAuth> serviceAuthentications = createServiceAuthentications(supportedAuthConfigs, authReadConfigs);
        return HttpGatewayRemoteServiceAuthenticator.fromRemoteClientAuthentications(serviceAuthentications);
    }

    @VisibleForTesting
    static List<HttpGatewayRemoteServiceAuth> createServiceAuthentications(List<HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs, Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs) {
        Map<String, Function<? extends HttpGatewayAuthObject, HttpGatewayUpstreamAuthenticator>> indexedAuthenticatorCreator = supportedAuthConfigs
            .stream()
            .collect(Collectors.toMap(
                authConfig -> authConfig.getAuthConfig().getAuthType(),
                HttpGatewayServiceAuthConfig::getAuthenticatorCreator
            ));

        return authReadConfigs
            .entrySet()
            .stream()
            .flatMap(serviceAuthByType -> serviceAuthByType.getValue().stream().map(serviceAuth -> new HttpGatewayRemoteServiceAuth(
                serviceAuth.getObjectId(),
                createAuthenticator(serviceAuthByType.getKey(), serviceAuth, indexedAuthenticatorCreator)
            )))
            .collect(Collectors.toList());
    }

    private static HttpGatewayUpstreamAuthenticator createAuthenticator(
        String authType,
        HttpGatewayAuthObject serviceAuth,
        Map<String, Function<? extends HttpGatewayAuthObject, HttpGatewayUpstreamAuthenticator>> indexedAuthenticatorCreator
    ) {
        //noinspection unchecked
        return ((Function<HttpGatewayAuthObject, HttpGatewayUpstreamAuthenticator>) indexedAuthenticatorCreator.get(authType)).apply(serviceAuth);
    }

    @Value(staticConstructor = "of")
    public static class HttpGatewayServiceAuthConfig<T extends HttpGatewayAuthObject> {
        HttpGatewayConfigAuth.HttpGatewayAuthConfig<T> authConfig;
        Function<T, HttpGatewayUpstreamAuthenticator> authenticatorCreator;
    }
}
