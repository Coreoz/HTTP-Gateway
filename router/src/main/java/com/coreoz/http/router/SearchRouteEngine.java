package com.coreoz.http.router;

import com.coreoz.http.router.data.*;
import com.coreoz.http.router.routes.HttpRoutes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handle route search in an {@link IndexedEndpoints}. Some examples (URI -> route):<br>
 * <pre>
 * - /users                             -> /users
 * - /users/1234                        -> /users/{id}
 * - /users/1234/addresses              -> /users/{id}/addresses
 * - /users/1234/addresses/5678         -> /users/{id}/addresses/{idAddress}
 * </pre>
 */
public class SearchRouteEngine {
    /**
     * Perform the search in the index.
     * @param indexEndpoints The route index
     * @param requestPath The path to search (that must start with a slash: "/")
     * @return The optional route that has been found
     */
    public static @NotNull Optional<MatchingRoute> searchRoute(@NotNull IndexedEndpoints indexEndpoints, @NotNull String requestPath) {
        ArrayDeque<String> requestElements = new ArrayDeque<>(Arrays.asList(requestPath.substring(1).split(HttpRoutes.SEGMENT_SEPARATOR)));
        List<SearchSegment> segmentOptions = new ArrayList<>();
        segmentOptions.add(new SearchSegment(
            indexEndpoints,
            requestElements,
            new HashMap<>()
        ));

        while (!segmentOptions.isEmpty()) {
            SearchSegment currentEndpointsOption = segmentOptions.remove(0);
            IndexedEndpoints indexedEndpoints = currentEndpointsOption.getIndexedEndpoints();

            if (currentEndpointsOption.getRequestRemainingSegments().isEmpty() && indexedEndpoints.getLastEndpoint() != null) {
                return Optional.of(
                    new MatchingRoute(
                        indexedEndpoints.getLastEndpoint(),
                        currentEndpointsOption.getParams()
                    )
                );
            }

            if (!currentEndpointsOption.getRequestRemainingSegments().isEmpty()) {
                String currentRequest = currentEndpointsOption.getRequestRemainingSegments().remove();
                if (indexedEndpoints.getSegments().get(currentRequest) != null) {
                    segmentOptions.add(toSearchSegment(indexedEndpoints.getSegments().get(currentRequest), currentEndpointsOption));
                }
                if (indexedEndpoints.getPattern() != null) {
                    currentEndpointsOption.getParams().put(currentEndpointsOption.getIndexedEndpoints().getDepth() + 1, currentRequest);
                    segmentOptions.add(toSearchSegment(indexedEndpoints.getPattern(), currentEndpointsOption));
                }
                segmentOptions
                    .sort(Comparator.comparingLong((SearchSegment searchSegment) -> searchSegment.getIndexedEndpoints().getRating())
                    .reversed());
            }

        }
        return Optional.empty();
    }

    private static @NotNull SearchSegment toSearchSegment(@NotNull IndexedEndpoints indexedEndpoints, @NotNull SearchSegment currentSegmentOption) {
        return new SearchSegment(
            indexedEndpoints,
            currentSegmentOption.getRequestRemainingSegments().clone(),
            currentSegmentOption.getParams()
        );
    }

    public static @NotNull DestinationRoute computeDestinationRoute(@NotNull MatchingRoute matchingRoute, @Nullable String destinationBaseUrl) {
        EndpointParsedData matchingEndpoint = matchingRoute.getMatchingEndpoint();
        @NotNull String serializedParsedPath = HttpRoutes.serializeParsedPath(
            matchingEndpoint.getDestinationRouteSegments(),
            currentSegmentName -> matchingRoute.getParams().get(matchingEndpoint.getPatterns().get(currentSegmentName))
        );
        return new DestinationRoute(
            matchingEndpoint.getHttpEndpoint().getRouteId(),
            destinationBaseUrl == null ? serializedParsedPath : (destinationBaseUrl + serializedParsedPath)
        );
    }
}
