package com.coreoz.http.openapi.service;

import com.coreoz.http.upstreamauth.HttpGatewayUpstreamAuthenticator;

// TODO to delete / review
public record OpenApiUpstreamParameters(String serviceId,
                                        HttpGatewayUpstreamAuthenticator upstreamAuthenticator,
                                        String openApiRemotePath
) {}
