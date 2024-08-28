package com.coreoz.http.router;

import com.coreoz.http.routes.parsing.ParsedSegment;
import com.coreoz.http.routes.router.HttpRoute;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record HttpGatewayRoute (String routeId, String method, String downstreamPath, List<ParsedSegment> upstreamParsedPath) implements HttpRoute {
    @Override
    public @NotNull String path() {
        return downstreamPath;
    }
}
