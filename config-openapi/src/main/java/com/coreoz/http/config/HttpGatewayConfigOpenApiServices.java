package com.coreoz.http.config;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.service.OpenApiUpstreamParameters;
import com.google.common.base.Predicates;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read OpenAPI service configuration
 */
public class HttpGatewayConfigOpenApiServices {
    public static final String CONFIG_OPEN_API_PREFIX = "open-api";

    // TODO read fetcher

    public static List<OpenApiUpstreamParameters> readConfig(HttpGatewayConfigLoader configLoader) {
        return readConfig(configLoader.getHttpGatewayConfig());
    }

    public static List<OpenApiUpstreamParameters> readConfig(HttpGatewayConfigLoader configLoader, List<HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        return readConfig(configLoader.getHttpGatewayConfig(), supportedAuthConfigs);
    }

    public static List<OpenApiUpstreamParameters> readConfig(Config gatewayConfig) {
        return readConfig(gatewayConfig, HttpGatewayConfigServicesAuth.supportedAuthConfigs());
    }

    public static List<OpenApiUpstreamParameters> readConfig(Config gatewayConfig, List<HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs) {
        Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs = HttpGatewayConfigServicesAuth.indexAuthenticationConfigs(supportedAuthConfigs);
        return HttpGatewayConfigServices
            .readRemoteServicesConfig(gatewayConfig)
            .stream()
            .map(serviceConfig -> {
                String serviceId = serviceConfig.getString(HttpGatewayConfigServices.CONFIG_SERVICE_ID);
                if (!serviceConfig.hasPath(CONFIG_OPEN_API_PREFIX)) {
                    return null;
                }
                // TODO replace by fetcher creation
                return new OpenApiUpstreamParameters(
                    serviceId,
                    HttpGatewayConfigServicesAuth.readRemoteServiceAuthentication(serviceConfig, indexedAuthConfigs),
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
