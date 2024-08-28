package com.coreoz.http.validation;

import com.coreoz.http.routes.parsing.DestinationRoute;
import com.coreoz.http.services.HttpGatewayRemoteService;
import org.jetbrains.annotations.NotNull;

public record HttpGatewayDestination(@NotNull DestinationRoute destinationRoute, @NotNull HttpGatewayRemoteService remoteService) {
}
