package com.coreoz.http.config;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.typesafe.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The function called on a service config object to create the OpenApiFetcher for the service.<br>
 * See {@link HttpGatewayConfigOpenApiServices#readResourceConfig(String, Config, HttpGatewayUpstreamClient, Map)}
 * for an example.
 */
@FunctionalInterface
public interface OpenApiConfigFetcherReader {
    /**
     * Create a {@link OpenApiFetcher} from a service config object
     * @param serviceId The service ID of the service config object
     * @param serviceConfig The service config object
     * @param upstreamClient The HTTP client to query remote OpenAPI file
     * @param indexedAuthConfigs The indexed authentication config readers available
     * @return The new {@link OpenApiFetcher} for the current service
     */
    @NotNull OpenApiFetcher readConfig(
        @NotNull String serviceId,
        @NotNull Config serviceConfig,
        @NotNull HttpGatewayUpstreamClient upstreamClient,
        @NotNull Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs
    );
}
