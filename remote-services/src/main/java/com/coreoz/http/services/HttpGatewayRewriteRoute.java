package com.coreoz.http.services;

import org.jetbrains.annotations.NotNull;

public record HttpGatewayRewriteRoute(@NotNull String routeId, @NotNull String downstreamPath) {
}
