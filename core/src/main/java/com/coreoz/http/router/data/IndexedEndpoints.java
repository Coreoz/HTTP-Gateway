package com.coreoz.http.router.data;

import lombok.*;

import java.util.Map;

@AllArgsConstructor
@Data
public class IndexedEndpoints<T> {
    private EndpointParsedData<T> lastEndpoint;
    private long rating;
    private int depth;
    private Map<String, IndexedEndpoints<T>> segments;
    private IndexedEndpoints<T> pattern;
}
