package com.coreoz.http.openapi.fetching;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface OpenApiFetcher {
    @NotNull CompletableFuture<OpenApiFetchingData> fetch();
}
