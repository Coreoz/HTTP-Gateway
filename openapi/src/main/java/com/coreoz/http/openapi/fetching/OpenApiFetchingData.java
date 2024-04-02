package com.coreoz.http.openapi.fetching;

import io.swagger.v3.oas.models.OpenAPI;
import org.jetbrains.annotations.NotNull;

/**
 * An OpenAPI specification
 * @param serviceId
 * @param openApiDefinition
 */
public record OpenApiFetchingData(@NotNull String serviceId, @NotNull OpenAPI openApiDefinition) {
}
