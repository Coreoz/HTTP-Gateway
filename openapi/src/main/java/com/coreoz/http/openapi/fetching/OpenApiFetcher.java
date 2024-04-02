package com.coreoz.http.openapi.fetching;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A function that will fetch an OpenAPI specification file for a service.<br>
 * The OpenAPI file can be reachable on an HTTP endpoint, in the project resources folder, or elsewhere.
 */
@FunctionalInterface
public interface OpenApiFetcher {
    /**
     * Returns a {@link CompletableFuture} that should not contain an exceptional case.<br>
     * Null should be returned if an issue occurs while fetching the OpenAPI specification file
     */
    @Nullable CompletableFuture<OpenApiFetchingData> fetch();
}
