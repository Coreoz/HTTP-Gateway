package com.coreoz.http.router.routes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a route that has been parsed. E.g <code>GET /path/{path-arg}/other-path-segment</code>.
 * @param parsedPath See {@link ParsedPath}
 * @param httpMethod The HTTP method used for the path. E.g. GET, POST, PUT, DELETE, PATCH, etc.
 * @param attachedData Some data that can be added to the route to ease later usage
 * @param <T> The type of {@link #attachedData}
 */
public record ParsedRoute<T>(@NotNull ParsedPath parsedPath, @NotNull String httpMethod,
                             @Nullable T attachedData) {
}
