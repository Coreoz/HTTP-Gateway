package com.coreoz.http.openapi.service;

import io.swagger.v3.oas.models.OpenAPI;

import java.util.concurrent.CompletableFuture;

public interface OpenApiFetchingService {
    CompletableFuture<OpenAPI> fetch();
}
