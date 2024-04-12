package com.coreoz.http.openapi.fetching;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility code for OpenAPI Fetchers
 */
@Slf4j
public class OpenApiFetchers {
    /**
     * Parse an OpenAPI file content
     * @param openApiContent The OpenAPI {@link String} definition
     * @param serviceId The name of the service linked to the OpenAPI definition
     * @return The {@link OpenAPI} parsed content, or null if it could not be parsed
     */
    @Nullable
    public static OpenAPI parseOpenApiContent(@NotNull String openApiContent, @NotNull String serviceId) {
        SwaggerParseResult openApiParsingResult = new OpenAPIParser().readContents(openApiContent, null, null);
        // when a parsing error occurs, no exception is thrown, just some loggers
        if (openApiParsingResult.getOpenAPI() == null) {
            logger.error(
                "Failed to parse Open API result for service '{}', content to be parsed={}",
                serviceId,
                openApiContent
            );
            return null;
        }
        return openApiParsingResult.getOpenAPI();
    }
}
