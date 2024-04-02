package com.coreoz.http.openapi.fetching.http;

import com.coreoz.http.upstream.HttpGatewayUpstreamClient;
import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for {@link OpenApiHttpFetcher}
 * @param serviceId
 * @param remoteUrl
 * @param upstreamClient
 * @param upstreamAuthenticator
 */
public record OpenApiHttpFetcherConfiguration(@NotNull String serviceId,
                                              @NotNull String remoteUrl,
                                              @NotNull HttpGatewayUpstreamClient upstreamClient,
                                              @Nullable HttpGatewayUpstreamAuthenticator upstreamAuthenticator) {}
