package com.coreoz.http.config;

import com.coreoz.http.access.control.auth.HttpGatewayAuthObject;
import com.coreoz.http.openapi.service.OpenApiUpstreamParameters;
import com.coreoz.http.services.auth.HttpGatewayRemoteServiceAuth;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import com.google.common.base.Predicates;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.coreoz.http.config.HttpGatewayConfigServicesAuth.createServiceAuthentications;

/**
 * Read OpenAPI service configuration
 */
public class HttpGatewayConfigOpenApiServices {
    public static final String CONFIG_OPEN_API_PREFIX = "open-api";

    public static List<OpenApiUpstreamParameters> readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static List<OpenApiUpstreamParameters> readConfig(HttpGatewayConfigLoader configLoader, List<HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        return readConfig(configLoader.getHttpGatewayConfig(), supportedAuthConfigs);
    }

    public static List<OpenApiUpstreamParameters> readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, HttpGatewayConfigServicesAuth.supportedAuthConfigs());
    }

    public static List<OpenApiUpstreamParameters> readConfig(Config gatewayConfig, List<HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<? extends HttpGatewayAuthObject>> supportedAuthConfigs) {
        Map<String, List<? extends HttpGatewayAuthObject>> authReadConfigs = HttpGatewayConfigAuth.readAuth(
            HttpGatewayConfigServices.CONFIG_SERVICE_ID,
            CONFIG_OPEN_API_PREFIX + ".",
            HttpGatewayConfigServices.readRemoteServicesConfig(gatewayConfig),
            supportedAuthConfigs.stream().map(HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig::getAuthConfig).collect(Collectors.toList())
        );
        Map<String, HttpGatewayUpstreamAuthenticator> serviceAuthentications = createServiceAuthentications(supportedAuthConfigs, authReadConfigs)
            .stream()
            .collect(Collectors.toMap(
                HttpGatewayRemoteServiceAuth::getServiceId,
                HttpGatewayRemoteServiceAuth::getAuthenticator
            ));
        return HttpGatewayConfigServices.readRemoteServicesConfig(gatewayConfig)
            .stream()
            .map(serviceConfig -> {
                String serviceId = serviceConfig.getString(HttpGatewayConfigServices.CONFIG_SERVICE_ID);
                if (!serviceConfig.hasPath(CONFIG_OPEN_API_PREFIX)) {
                    return null;
                }
                return new OpenApiUpstreamParameters(
                    serviceId,
                    serviceAuthentications.get(serviceId),
                    readConfigOptionalValue(serviceConfig, CONFIG_OPEN_API_PREFIX + ".remote-path")
                );
            })
            .filter(Predicates.notNull())
            .collect(Collectors.toList());

    }

    private static String readConfigOptionalValue(Config config, String configKey) {
        if (config.hasPath(configKey)) {
            return config.getString(configKey);
        }
        return null;
    }
}
