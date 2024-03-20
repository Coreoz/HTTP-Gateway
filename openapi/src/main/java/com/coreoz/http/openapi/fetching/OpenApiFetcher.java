package com.coreoz.http.openapi.fetching;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface OpenApiFetcher {
    @Nullable CompletableFuture<OpenApiFetchingData> fetch();
}
