package com.coreoz.http.router.routes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Helper class to validate and parse routes. A typical use case is to verify that a new route is not already defined.
 * @param <T> If needed, the type of data that is being attached to a route
 */
public class HttpRoutesValidator<T> {
    private final Map<String, List<ParsedRoute<T>>> existingRoutes = new HashMap<>();

    /**
     * Verify if a route exists for a path and an HTTP method
     * @param path The path to search route
     * @param httpMethod The HTTP method to search route
     * @return True if a route exists, else false
     */
    public boolean hasRoute(@NotNull String path, @NotNull String httpMethod) {
        return findRoute(path, httpMethod) != null;
    }

    /**
     * Verify if a route exists for a path
     * @param path The path to search route
     * @return True if a route exists, else false
     */
    public boolean hasRoute(@NotNull String path) {
        return !findRoutes(path).isEmpty();
    }

    /**
     * Find the route matching the path and the HTTP method passed as parameter
     * @param path The path to search route
     * @return The routes matching the path
     */
    public @Nullable ParsedRoute<T> findRoute(@NotNull String path, @NotNull String httpMethod) {
        return findEndpoint(findRoutes(path), httpMethod).orElse(null);
    }

    /**
     * Find all the routes matching the path, one route will be returned for each existing HTTP method matching the path.
     * @param path The path to search route
     * @return The routes matching the path
     */
    public @NotNull List<ParsedRoute<T>> findRoutes(@NotNull String path) {
        return findRoutes(HttpRoutes.parsePath(path));
    }

    /**
     * Find all the routes matching the path, one route will be returned for each existing HTTP method matching the path.
     * @param path The path to search route
     * @return The routes matching the path
     */
    public @NotNull List<ParsedRoute<T>> findRoutes(@NotNull ParsedPath path) {
        return existingRoutes.getOrDefault(path.genericPath(), List.of());
    }

    /**
     * Add a route to the existing routes
     * @param path The path of the new route
     * @param httpMethod The HTTP method of the new route: GET, POST, PUT, etc.
     * @param attachedData Some optional data to be attached to the route, for further processing later
     * @return The corresponding {@link ParsedRoute} that has been added if no route existed for the path and http method.
     * If a route already exists, null is returned and the existing route is not changed
     */
    public @Nullable ParsedRoute<T> addRoute(@NotNull String path, @NotNull String httpMethod, @Nullable T attachedData) {
        return addRoute(HttpRoutes.parseRoute(path, httpMethod, attachedData));
    }

    /**
     * Add a route to the existing routes
     * @param route The route to add
     * @return The route passed as parameter if the route didn't exist yet, or null if the route already exists:
     * so the route passed as parameter is not added
     */
    public @Nullable ParsedRoute<T> addRoute(@NotNull ParsedRoute<T> route) {
        List<ParsedRoute<T>> availableEndpoints = existingRoutes.get(route.parsedPath().genericPath());
        if (availableEndpoints == null) {
            List<ParsedRoute<T>> endpoints = new ArrayList<>();
            endpoints.add(route);
            existingRoutes.put(route.parsedPath().genericPath(), endpoints);
            return route;
        }
        if (findEndpoint(availableEndpoints, route.httpMethod()).isPresent()) {
            return null;
        }
        availableEndpoints.add(route);
        return route;
    }

    private @NotNull Optional<ParsedRoute<T>> findEndpoint(@NotNull List<ParsedRoute<T>> availableEndpoints, @NotNull String httpMethod) {
        return availableEndpoints.stream().filter(endpoint -> endpoint.httpMethod().equals(httpMethod)).findFirst();
    }

    /**
     * Creates a {@link Collector} to use on a {@link java.util.stream.Stream} to reduce a Stream to a {@link HttpRoutesValidator}
     * @param routeExtractor The function that transforms a Stream element to {@link ParsedRoute}. The function {@link HttpRoutes#parseRoute(String, String, Object)} should generally be used to that.
     * @return The corresponding {@link HttpRoutesValidator}
     * @param <E> The type of the {@link java.util.stream.Stream} elements
     * @param <T> The type of the resulted {@link HttpRoutesValidator}
     */
    public static <E, T> Collector<E, HttpRoutesValidator<T>, HttpRoutesValidator<T>> collector(@NotNull RouteExtractor<E, T> routeExtractor) {
        return Collector.of(
            HttpRoutesValidator::new,
            (routesValidator, element) -> routesValidator.addRoute(routeExtractor.extractRoute(element)),
            (a, b) -> {
                throw new RuntimeException("Parallel stream collection is not supported");
            },
            Function.identity(),
            Collector.Characteristics.UNORDERED, Collector.Characteristics.IDENTITY_FINISH
        );
    }

    /**
     * Transform a {@link java.util.stream.Stream} element to a {@link ParsedRoute} element
     * @param <E> The type of the Stream elements
     * @param <T> The type parameter of {@link ParsedRoute}
     */
    @FunctionalInterface
    public interface RouteExtractor<E, T> {
        @NotNull ParsedRoute<T> extractRoute(E streamElement);
    }
}
