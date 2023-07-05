package com.coreoz.router;

import com.coreoz.router.data.HttpEndpoint;
import com.coreoz.router.data.EndpointParsedData;
import com.coreoz.router.data.IndexedEndpoints;
import com.coreoz.router.data.ParsedSegment;
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
                    logger.warn("Le endpoint '{}' contient un segment incorrect : vide ou avec un pattern vide", endpoint);
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
    public static <T> EndpointParsedData<T> addEndpointToIndex(Map<String, IndexedEndpoints<T>> indexedEndpoints, HttpEndpoint<T> endpoint) {
        IndexedEndpoints<T> rootIndex = indexedEndpoints.computeIfAbsent(endpoint.getMethod(), method -> IndexedEndpoints.of(
            null,
            1L << MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS,
            0,
            new HashMap<>(),
            null
        ));

        // parser la route
        List<ParsedSegment> segments = parseEndpoint(endpoint.getLocalPath());
        // initialise patterns map
        Map<String, Integer> patterns = new HashMap<>();

        IndexedEndpoints<T> currentIndex = rootIndex;
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
                    logger.warn("Deux routes sont en conflit (la dernière ne sera pas ajoutée) : {} et {}", currentIndex.getLastEndpoint().getHttpEndpoint().getLocalPath(), endpoint.getLocalPath());
                    return currentIndex.getLastEndpoint();
                }
                EndpointParsedData<T> newEndpoint = EndpointParsedData.of(
                    patterns,
                    parseEndpoint(endpoint.getDestinationPath()),
                    endpoint
                );
                currentIndex.setLastEndpoint(newEndpoint);
                return newEndpoint;
            }
        }

        logger.error("Cas normalement impossible avec l'ajout du endpoint vide {}", endpoint);
        return null;
    }

    private static <T> IndexedEndpoints<T> computeSegmentIndex(IndexedEndpoints<T> currentIndex, String segmentName, int segmentIndex) {
        return currentIndex.getSegments().computeIfAbsent(segmentName, (segmentNameToAdd) -> IndexedEndpoints.of(
            null,
            currentIndex.getRating() | 1L << (MAX_LONG_OFFSET_FOR_POSITIVE_NUMBERS - segmentIndex),
            segmentIndex,
            new HashMap<>(),
            null
        ));
    }

    private static <T> IndexedEndpoints<T> computePatternIndex(IndexedEndpoints<T> currentIndex, String segmentName, int segmentIndex, Map<String, Integer> patterns) {
        patterns.put(segmentName, segmentIndex);
        if (currentIndex.getPattern() == null) {
            IndexedEndpoints<T> pattern = IndexedEndpoints.of(
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
    public static <T> Map<String, IndexedEndpoints<T>> indexEndpoints(List<HttpEndpoint<T>> endpoints) {
        // 1. on construit le résultat final
        Map<String, IndexedEndpoints<T>> indexedEndpoints = new HashMap<>();
        // 2. on boucle sur les endpoints et on les ajoute
        for (HttpEndpoint<T> endpoint : endpoints) {
            addEndpointToIndex(indexedEndpoints, endpoint);
        }
        // 3. on retourne le résultat
        return indexedEndpoints;
    }
}
