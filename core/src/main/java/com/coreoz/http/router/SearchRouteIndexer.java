package com.coreoz.http.router;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.EndpointParsedData;
import com.coreoz.http.router.data.IndexedEndpoints;
import com.coreoz.http.router.data.ParsedSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gère l'indexation des motifs de routes. Par exemple :
 * - /users
 * - /users/{id}
 * - /users/{id}/addresses
 * - /users/{id}/addresses/{idAddress}
 *
 * Les routes indexées peuvent ensuite être recherchées avec @{@link SearchRouteEngine}
 */
public class SearchRouteIndexer {
    private static final Logger logger = LoggerFactory.getLogger(SearchRouteIndexer.class);

    private static final Integer MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS = 62;

    /**
     * Parse the segments of an endpoint. For instance <code>/users/{id}/addresses</code> will give:
     * <pre>
     * - users (pattern = false)
     * - id (pattern = true)
     * - addresses (pattern = false)
     * </pre>
     */
    public static List<ParsedSegment> parseEndpoint(String endpoint) {
        return Arrays.stream(endpoint.substring(1).split("/")) // substring : pour un endpoint /test/truc le split renvoie ['', 'test', 'truc'], il faut supprimer le premier élément
            .map(segment -> {
                boolean isPattern = segment.length() >= 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
                String name = isPattern ?
                    segment.substring(1, segment.length() - 1) :
                    segment;
                if (name.isEmpty()) {
                    logger.warn("The endpoint '{}' contains an incorrect segment: empty or with an empty pattern", endpoint);
                }
                return new ParsedSegment(name, isPattern);
            })
            .collect(Collectors.toList());
    }

    /**
     * Ajoute un nouveau endpoint à l'arbre des routes.
     *
     * Retourne la nouvelle route ajoutée à l'arbre
     * ou la route existante qui était déjà présente dans l'arbre.
     */
    public static  EndpointParsedData addEndpointToIndex(Map<String, IndexedEndpoints> indexedEndpoints, HttpEndpoint endpoint) {
        IndexedEndpoints rootIndex = indexedEndpoints.computeIfAbsent(endpoint.getMethod(), method -> new IndexedEndpoints(
            null,
            1L << MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS,
            0,
            new HashMap<>(),
            null
        ));

        // parser la route
        List<ParsedSegment> segments = parseEndpoint(endpoint.getDownstreamPath());
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
                    parseEndpoint(endpoint.getUpstreamPath()),
                    endpoint
                );
                currentIndex.setLastEndpoint(newEndpoint);
                return newEndpoint;
            }
        }

        logger.error("The endpoint {} could not be added, this is a bug", endpoint);
        return null;
    }

    private static  IndexedEndpoints computeSegmentIndex(IndexedEndpoints currentIndex, String segmentName, int segmentIndex) {
        return currentIndex.getSegments().computeIfAbsent(segmentName, (segmentNameToAdd) -> new IndexedEndpoints(
            null,
            currentIndex.getRating() | 1L << (MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS - segmentIndex),
            segmentIndex,
            new HashMap<>(),
            null
        ));
    }

    private static  IndexedEndpoints computePatternIndex(IndexedEndpoints currentIndex, String segmentName, int segmentIndex, Map<String, Integer> patterns) {
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
    public static  Map<String, IndexedEndpoints> indexEndpoints(Iterable<HttpEndpoint> endpoints) {
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
