package com.coreoz.http.config;

import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

// TODO docs
@Setter
@Getter
@Accessors(fluent = true)
public class HttpGatewayConfigOpenApiServicesParameters {
    public static final Map<String, OpenApiConfigFetcherReader> DEFAULT_SUPPORTED_FETCHER_READERS = Map.ofEntries(
        HttpGatewayConfigOpenApiServices.FETCHER_REMOTE_PATH,
        HttpGatewayConfigOpenApiServices.FETCHER_REMOTE_URL,
        HttpGatewayConfigOpenApiServices.FETCHER_RESOURCE
    );

    @NotNull private final Config gatewayConfig;
    @NotNull private HttpGatewayUpstreamClient upstreamClient = new HttpGatewayUpstreamClient();
    @NotNull private Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> supportedAuthConfigs = HttpGatewayConfigServicesAuth.supportedAuthConfigs();
    @NotNull private Map<String, OpenApiConfigFetcherReader> supportedFetcherReaders = DEFAULT_SUPPORTED_FETCHER_READERS;

    public HttpGatewayConfigOpenApiServicesParameters(@NotNull Config gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    public HttpGatewayConfigOpenApiServicesParameters(@NotNull HttpGatewayConfigLoader configLoader) {
        this.gatewayConfig = configLoader.getHttpGatewayConfig();
    }
}
