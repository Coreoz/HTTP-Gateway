package com.coreoz.http.router;

import com.coreoz.http.router.data.*;

import java.util.*;

/**
 * Gère la recherche d'une route concrète auprès d'un index de motifs. Par exemple :
 * - /users                             -> /users
 * - /users/1234                        -> /users/{id}
 * - /users/1234/addresses              -> /users/{id}/addresses
 * - /users/1234/addresses/5678         -> /users/{id}/addresses/{idAddress}
 *
 * L'index des motifs de routes est créé avec @{@link SearchRouteIndexer}
 */
public class SearchRouteEngine {
    private static final String SEGMENT_SEPARATOR = "/";

    public static Optional<MatchingRoute> searchRoute(IndexedEndpoints indexEndpoints, String requestPath) {
        // initialisation
        //substring(1) permet d'enlever le premier /
        ArrayDeque<String> requestElements = new ArrayDeque<>(Arrays.asList(requestPath.substring(1).split(SEGMENT_SEPARATOR)));
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

    private static  SearchSegment toSearchSegment(IndexedEndpoints indexedEndpoints, SearchSegment currentSegmentOption) {
        return new SearchSegment(
            indexedEndpoints,
            currentSegmentOption.getRequestRemainingSegments().clone(),
            currentSegmentOption.getParams()
        );
    }

    public static  DestinationRoute computeDestinationRoute(MatchingRoute matchingRoute) {
        StringBuilder result = new StringBuilder();
        EndpointParsedData matchingEndpoint = matchingRoute.getMatchingEndpoint();
        for (int segmentIndex = 1; segmentIndex <= matchingEndpoint.getDestinationRouteSegments().size(); segmentIndex++) {
            ParsedSegment currentSegment = matchingEndpoint.getDestinationRouteSegments().get(segmentIndex - 1);
            if (segmentIndex < matchingEndpoint.getDestinationRouteSegments().size() + 1) {
                result.append(SEGMENT_SEPARATOR);
            }
            if (currentSegment.isPattern()) {
                result.append(matchingRoute.getParams().get(matchingEndpoint.getPatterns().get(currentSegment.getName())));
            } else {
                result.append(currentSegment.getName());
            }
        }
        return new DestinationRoute(
            matchingEndpoint.getHttpEndpoint().getRouteId(),
            matchingEndpoint.getHttpEndpoint().getDestinationBaseUrl() + result.toString()
        );
    }
}
