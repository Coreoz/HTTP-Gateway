package com.coreoz.http.config;

import com.coreoz.http.exception.HttpGatewayValidationException;
import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcherConfiguration;
import com.coreoz.http.openapi.fetching.resources.OpenApiResourceFetcher;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.google.common.base.Predicates;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read OpenAPI service configuration
 */
public class HttpGatewayConfigOpenApiServices {
    public static final Map.Entry<String, OpenApiConfigFetcherReader> FETCHER_REMOTE_PATH = Map.entry("remote-path", HttpGatewayConfigOpenApiServices::readRemotePathConfig);
    public static final Map.Entry<String, OpenApiConfigFetcherReader> FETCHER_REMOTE_URL = Map.entry("remote-url", HttpGatewayConfigOpenApiServices::readRemoteUrlConfig);
    public static final Map.Entry<String, OpenApiConfigFetcherReader> FETCHER_RESOURCE = Map.entry("resource-path", HttpGatewayConfigOpenApiServices::readResourceConfig);

    public static final String CONFIG_OPEN_API_PREFIX = "open-api";

    /**
     * Create a list of {@link OpenApiFetcher} from an HTTP Gateway config and optional customization parameters:
     * see {@link HttpGatewayConfigOpenApiServicesParameters} for details.<br>
     * <br>
     * For a service, if the OpenAPI object is missing, then no fetcher will be created for this service.
     * @param openApiConfigParameters The HTTP Gateway config object containing services configuration and option parameters
     * @return The corresponding list of {@link OpenApiFetcher}
     */
    public static @NotNull List<OpenApiFetcher> readConfig(@NotNull HttpGatewayConfigOpenApiServicesParameters openApiConfigParameters) {
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
                return fetcherReader.readConfig(
                    serviceId,
                    serviceConfig,
                    openApiConfigParameters.upstreamClient(),
                    openApiConfigParameters.supportedAuthConfigs()
                );
            })
            .filter(Predicates.notNull())
            .collect(Collectors.toList());
    }

    private static @NotNull OpenApiFetcher readRemotePathConfig(@NotNull String serviceId, @NotNull Config serviceConfig, @NotNull HttpGatewayUpstreamClient upstreamClient, @NotNull Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs) {
        return buildRemoteUrlFetcher(serviceId,
            serviceConfig,
            serviceConfig.getString("base-url") + serviceConfig.getString(CONFIG_OPEN_API_PREFIX + ".remote-path"),
            upstreamClient,
            indexedAuthConfigs
        );
    }

    private static @NotNull OpenApiFetcher readRemoteUrlConfig(@NotNull String serviceId, @NotNull Config serviceConfig, @NotNull HttpGatewayUpstreamClient upstreamClient, @NotNull Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs) {
        return buildRemoteUrlFetcher(serviceId,
            serviceConfig,
            serviceConfig.getString(CONFIG_OPEN_API_PREFIX + ".remote-url"),
            upstreamClient,
            indexedAuthConfigs
        );
    }

    private static @NotNull OpenApiFetcher buildRemoteUrlFetcher(@NotNull String serviceId, @NotNull Config serviceConfig, @NotNull String remoteUrl, @NotNull HttpGatewayUpstreamClient upstreamClient, @NotNull Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs) {
        return new OpenApiHttpFetcher(new OpenApiHttpFetcherConfiguration(
            serviceId,
            remoteUrl,
            upstreamClient,
            HttpGatewayConfigServicesAuth.readRemoteServiceAuthentication(serviceConfig.getConfig(CONFIG_OPEN_API_PREFIX), indexedAuthConfigs)
        ));
    }

    private static @NotNull OpenApiFetcher readResourceConfig(@NotNull String serviceId, @NotNull Config serviceConfig, @NotNull HttpGatewayUpstreamClient upstreamClient, @NotNull Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs) {
        return new OpenApiResourceFetcher(serviceId, serviceConfig.getString(CONFIG_OPEN_API_PREFIX + ".resource-path"));
    }
}
