package com.coreoz.http.services;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record HttpGatewayRemoteService(@NotNull String serviceId, @NotNull String baseUrl, @NotNull List<HttpGatewayRemoteServiceRoute> routes) {
}
