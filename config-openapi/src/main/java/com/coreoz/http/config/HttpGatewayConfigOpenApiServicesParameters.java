package com.coreoz.http.config;

import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * OpenAPI configuration parameters.<br>
 * <br>
 * These parameters have default value (except for the {@link Config} object itself). IT is possible to set
 * custom value for each parameter.
 */
@Setter
@Getter
@Accessors(fluent = true)
public class HttpGatewayConfigOpenApiServicesParameters {
    public static final Map<String, OpenApiConfigFetcherReader> DEFAULT_SUPPORTED_FETCHER_READERS = Map.ofEntries(
        HttpGatewayConfigOpenApiServices.FETCHER_REMOTE_PATH,
        HttpGatewayConfigOpenApiServices.FETCHER_REMOTE_URL,
        HttpGatewayConfigOpenApiServices.FETCHER_RESOURCE
    );

    /**
     * The HTTP Gateway config object
     */
    @NotNull private final Config gatewayConfig;
    /**
     * The HTTP client used to query remote OpenAPI endpoints
     */
    @NotNull private HttpGatewayUpstreamClient upstreamClient = new HttpGatewayUpstreamClient();
    /**
     * The remote OpenAPI endpoint supported authentication.<br>
     * This needs to be customized if a remote authentication is not supported by default yet in the HTTP Gateway. E.g. with OAuth2.
     */
    @NotNull private Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs = HttpGatewayConfigServicesAuth.supportedAuthConfigs();
    /**
     * The adapters used to fetch the OpenAPI files.<br>
     * This needs to be customized if a remote OpenAPI file method is not yet supported in the HTTP Gateway. E.g. FTP.
     */
    @NotNull private Map<String, OpenApiConfigFetcherReader> supportedFetcherReaders = DEFAULT_SUPPORTED_FETCHER_READERS;

    public HttpGatewayConfigOpenApiServicesParameters(@NotNull Config gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    public HttpGatewayConfigOpenApiServicesParameters(@NotNull HttpGatewayConfigLoader configLoader) {
        this.gatewayConfig = configLoader.getHttpGatewayConfig();
    }
}
