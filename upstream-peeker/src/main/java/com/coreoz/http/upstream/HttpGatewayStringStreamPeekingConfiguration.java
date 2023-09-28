package com.coreoz.http.upstream;

import lombok.Value;

/**
 * Configuration for {@link HttpGatewayUpstreamStringPeekerClient}
 */
@Value
public class HttpGatewayStringStreamPeekingConfiguration {
    public static final int DEFAULT_MAX_BYTES_TO_PEEK = 10_000;
    public static final HttpGatewayStringStreamPeekingConfiguration DEFAULT_CONFIG = new HttpGatewayStringStreamPeekingConfiguration(DEFAULT_MAX_BYTES_TO_PEEK);

    int maxBytesToPeek;
}
