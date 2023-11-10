package com.coreoz.http.router.data;

import lombok.*;

import java.util.Map;

@AllArgsConstructor
@Data
public class IndexedEndpoints {
    private EndpointParsedData lastEndpoint;
    private long rating;
    private int depth;
    private Map<String, IndexedEndpoints> segments;
    private IndexedEndpoints pattern;
}
