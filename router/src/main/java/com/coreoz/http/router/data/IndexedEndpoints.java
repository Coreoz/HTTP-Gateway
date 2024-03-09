package com.coreoz.http.router.data;

import lombok.*;

import java.util.Map;

/**
 * A routes index.<br>
 * A route contains a method (GET, POST, etc.), and a path (e.g. "/users/{userId}/addresses")<br>
 * See {@link com.coreoz.http.router.SearchRouteIndexer} for usage.
 */
@AllArgsConstructor
@Data
public class IndexedEndpoints {
    private EndpointParsedData lastEndpoint;
    private long rating;
    private int depth;
    private Map<String, IndexedEndpoints> segments;
    private IndexedEndpoints pattern;
}
