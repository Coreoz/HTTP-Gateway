package com.coreoz.http.router;

import com.coreoz.http.router.data.EndpointParsedData;
import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.IndexedEndpoints;
import com.coreoz.http.router.data.ParsedSegment;
import com.coreoz.http.router.routes.HttpRoutes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handle route indexing in an {@link IndexedEndpoints}. Some examples of routes:<br>
 * <pre>
 * - /users
 * - /users/{id}
 * - /users/{id}/addresses
 * - /users/{id}/addresses/{idAddress}
 * </pre>
 * {@link SearchRouteEngine} enables route search in the {@link IndexedEndpoints}.
 */
public class SearchRouteIndexer {
    private static final Integer MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS = 62;

    /**
     * Add a new endpoint to the routes tree.<br>
     * <br>
     * Returns the new route added to the tree or the existing route that is already present in the tree.
     */
    public static @NotNull EndpointParsedData addEndpointToIndex(@NotNull Map<String, IndexedEndpoints> indexedEndpoints, @NotNull HttpEndpoint endpoint) {
        IndexedEndpoints rootIndex = indexedEndpoints.computeIfAbsent(endpoint.getMethod(), method -> new IndexedEndpoints(
            null,
            1L << MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS,
            0,
            new HashMap<>(),
            null
        ));

        // parser la route
        List<ParsedSegment> segments = HttpRoutes.parsePathAsSegments(endpoint.getDownstreamPath());
        // initialise patterns map
        Map<String, Integer> patterns = new HashMap<>();

        IndexedEndpoints currentIndex = rootIndex;
        for (int segmentIndex = 1; segmentIndex <= segments.size(); segmentIndex++) {
            ParsedSegment parsedSegmentToAdd = segments.get(segmentIndex - 1);
            if (parsedSegmentToAdd.isPattern()) {
                currentIndex = computePatternIndex(currentIndex, parsedSegmentToAdd.getName(), segmentIndex, patterns);
            } else {
                currentIndex = computeSegmentIndex(currentIndex, parsedSegmentToAdd.getName(), segmentIndex);
            }

            // condition finale d'arrêt
            if (segmentIndex == segments.size()) {
                if (currentIndex.getLastEndpoint() != null) {
                    // cas possible /test/{bidule}/truc et /test/{machin}/truc
                    // There is already an existing route for the current route
                    // => The new route is not added and the existing route is returned
                    return currentIndex.getLastEndpoint();
                }
                EndpointParsedData newEndpoint = new EndpointParsedData(
                    patterns,
                    HttpRoutes.parsePathAsSegments(endpoint.getUpstreamPath()),
                    endpoint
                );
                currentIndex.setLastEndpoint(newEndpoint);
                return newEndpoint;
            }
        }

        throw new RuntimeException("The endpoint " + endpoint + " could not be added, this is a bug");
    }

    private static @NotNull IndexedEndpoints computeSegmentIndex(@NotNull IndexedEndpoints currentIndex, @NotNull String segmentName, int segmentIndex) {
        return currentIndex.getSegments().computeIfAbsent(segmentName, segmentNameToAdd -> new IndexedEndpoints(
            null,
            currentIndex.getRating() | 1L << (MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS - segmentIndex),
            segmentIndex,
            new HashMap<>(),
            null
        ));
    }

    private static @NotNull IndexedEndpoints computePatternIndex(
        @NotNull IndexedEndpoints currentIndex, @NotNull String segmentName, int segmentIndex, @NotNull Map<String, Integer> patterns
    ) {
        patterns.put(segmentName, segmentIndex);
        if (currentIndex.getPattern() == null) {
            IndexedEndpoints pattern = new IndexedEndpoints(
                null,
                currentIndex.getRating(),
                segmentIndex,
                new HashMap<>(),
                null
            );
            currentIndex.setPattern(pattern);
            return pattern;
        }
        return currentIndex.getPattern();
    }

    /**
     * Main indexation method
     */
    public static @NotNull Map<String, IndexedEndpoints> indexEndpoints(@NotNull Iterable<HttpEndpoint> endpoints) {
        // 1. on construit le résultat final
        Map<String, IndexedEndpoints> indexedEndpoints = new HashMap<>();
        // 2. on boucle sur les endpoints et on les ajoute
        for (HttpEndpoint endpoint : endpoints) {
            addEndpointToIndex(indexedEndpoints, endpoint);
        }
        // 3. on retourne le résultat
        return indexedEndpoints;
    }
}
