package com.coreoz.http.services.auth;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record HttpGatewayRemoteServiceAuth(@NotNull String serviceId, @Nullable HttpGatewayUpstreamAuthenticator authenticator) {
}
