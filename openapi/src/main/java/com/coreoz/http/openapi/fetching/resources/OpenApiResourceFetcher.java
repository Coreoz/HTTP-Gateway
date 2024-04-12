package com.coreoz.http.openapi.fetching.resources;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.OpenApiFetchers;
import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.google.common.io.Resources;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link OpenApiFetcher} that will fetch an OpenAPI file directly in the project resources folder.
 */
@Slf4j
public class OpenApiResourceFetcher implements OpenApiFetcher {
    private final @NotNull String serviceId;
    private final @NotNull String resourcePath;

    /**
     * Creates a new {@link OpenApiResourceFetcher}
     * @param serviceId The identifier of the service related to the OpenAPI file
     * @param resourcePath The resource path of the OpenAPI file. For example <code>/openapi/service-abcd-spec.yml</code>
     *                     for a file located in the <code>src/main/resources/openapi/service-abcd-spec.yml</code> location
     */
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
            OpenAPI openApiContent = OpenApiFetchers.parseOpenApiContent(resourceString, serviceId);
            return CompletableFuture.completedFuture(openApiContent == null ?
                null
                : new OpenApiFetchingData(
                    serviceId,
                    openApiContent
                ));
        } catch (Exception e) {
            logger.error("Failed to load openAPI definitions for service {}", serviceId, e);
            return CompletableFuture.completedFuture(null);
        }

    }
}
