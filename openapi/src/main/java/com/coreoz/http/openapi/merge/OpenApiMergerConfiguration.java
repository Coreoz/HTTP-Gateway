package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.data.HttpEndpoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @param endpoints
 * @param componentNamePrefix The prefix that will be used for all new component name that will be added. This is used to avoid conflict with existing components
 * @param operationIdPrefix Used if HttpEndpoint routeId is not present. The prefix that will be used for all new operation id that will be added. This is used to avoid conflict with existing operations
 * @param createMissingEndpoints True if endpoints present in the configuration and not in the OpenAPI should be added or not
 */
public record OpenApiMergerConfiguration(
    @NotNull List<HttpEndpoint> endpoints,
    @NotNull String componentNamePrefix,
    @NotNull String operationIdPrefix,
    boolean createMissingEndpoints
) { }
