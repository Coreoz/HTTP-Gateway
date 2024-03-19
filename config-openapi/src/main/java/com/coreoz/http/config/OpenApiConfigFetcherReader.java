package com.coreoz.http.config;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.typesafe.config.Config;

import java.util.Map;

// TODO docs
@FunctionalInterface
public interface OpenApiConfigFetcherReader {
    OpenApiFetcher readConfig(String serviceId, Config serviceConfig, HttpGatewayUpstreamClient upstreamClient, Map<String, HttpGatewayConfigServicesAuth.HttpGatewayServiceAuthConfig<?>> indexedAuthConfigs);
}
