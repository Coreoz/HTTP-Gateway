package com.coreoz.http.config;

import com.coreoz.http.exception.HttpGatewayValidationException;
import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcherConfiguration;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.google.common.base.Predicates;
import com.typesafe.config.Config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read OpenAPI service configuration
 */
public class HttpGatewayConfigOpenApiServices {
    // type = "remote-path"
    //        remote-url = "https://..."
    //        remote-path = "/openapi"
    //        resource-path = "/openapi.yaml"
    public static final Map.Entry<String, OpenApiConfigFetcherReader> FETCHER_REMOTE_PATH = Map.entry("remote-path", HttpGatewayConfigOpenApiServices::readRemotePathConfig);

    public static final String CONFIG_OPEN_API_PREFIX = "open-api";

    public static List<OpenApiFetcher> readConfig(HttpGatewayConfigOpenApiServicesParameters openApiConfigParameters) {
        Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs = HttpGatewayConfigServicesAuth.indexAuthenticationConfigs(openApiConfigParameters.supportedAuthConfigs());
        return HttpGatewayConfigServices
            .readRemoteServicesConfig(openApiConfigParameters.gatewayConfig())
            .stream()
            .map(serviceConfig -> {
                String serviceId = serviceConfig.getString(HttpGatewayConfigServices.CONFIG_SERVICE_ID);
                if (serviceId == null) {
                    throw new HttpGatewayValidationException("Wrong service config, missing  '" + HttpGatewayConfigServices.CONFIG_SERVICE_ID + "' field in " + serviceConfig);
                }
                if (!serviceConfig.hasPath(CONFIG_OPEN_API_PREFIX)) {
                    return null;
                }
                String fetcherType = serviceConfig.getString(CONFIG_OPEN_API_PREFIX + ".type");
                OpenApiConfigFetcherReader fetcherReader = openApiConfigParameters.supportedFetcherReaders().get(fetcherType);
                if (fetcherReader == null) {
                    throw new HttpGatewayValidationException("Unrecognized OpenAPI type  '" + fetcherType + "' for service " + serviceId + ", available types: " + openApiConfigParameters.supportedFetcherReaders().keySet());
                }
                return fetcherReader.readConfig(serviceId, serviceConfig, openApiConfigParameters.upstreamClient(), indexedAuthConfigs);
            })
            .filter(Predicates.notNull())
            .collect(Collectors.toList());
    }

    public static OpenApiFetcher readRemotePathConfig(String serviceId, Config serviceConfig, HttpGatewayUpstreamClient upstreamClient, Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs) {
        return new OpenApiHttpFetcher(new OpenApiHttpFetcherConfiguration(
            serviceId,
            serviceConfig.getString("base-url") + serviceConfig.getString(CONFIG_OPEN_API_PREFIX + ".remote-path"),
            upstreamClient,
            HttpGatewayConfigServicesAuth.readRemoteServiceAuthentication(serviceConfig.getConfig(CONFIG_OPEN_API_PREFIX), indexedAuthConfigs)
        ));
    }

    private static String readConfigOptionalValue(Config config, String configKey) {
        if (config.hasPath(configKey)) {
            return config.getString(configKey);
        }
        return null;
    }
}
