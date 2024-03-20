package com.coreoz.http.openapi.fetching.resources;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.google.common.io.Resources;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

// TODO docs
@Slf4j
public class OpenApiResourceFetcher implements OpenApiFetcher {
    private final @NotNull String serviceId;
    private final @NotNull String resourcePath;

    public OpenApiResourceFetcher(@NotNull String serviceId, @NotNull String resourcePath) {
        this.serviceId = serviceId;
        this.resourcePath = resourcePath;
    }

    @Override
    public @NotNull CompletableFuture<OpenApiFetchingData> fetch() {
        URL fileResource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        if (fileResource == null) {
            logger.error("No resource found for resource path: {}", resourcePath);
            return CompletableFuture.completedFuture(null);
        }
        try {
            String resourceString = Resources.toString(fileResource, StandardCharsets.UTF_8);
            SwaggerParseResult openApiParsingResult = new OpenAPIParser().readContents(resourceString, null, null);
            return CompletableFuture.completedFuture(new OpenApiFetchingData(
                serviceId,
                openApiParsingResult.getOpenAPI()
            ));
        } catch (Exception e) {
            logger.error("Failed to load openAPI definitions for service {}", serviceId, e);
            return CompletableFuture.completedFuture(null);
        }

    }
}
