package com.coreoz.http.services;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record HttpGatewayRemoteServiceRoute(@NotNull String routeId, @NotNull String method, @Nullable String path) {
}
