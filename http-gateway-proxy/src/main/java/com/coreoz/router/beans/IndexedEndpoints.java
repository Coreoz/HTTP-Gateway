package com.coreoz.router.beans;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class IndexedEndpoints<T> {
    private EndpointParsedData<T> lastEndpoint;
    private long rating;
    private int depth;
    private Map<String, IndexedEndpoints<T>> segments;
    private IndexedEndpoints<T> pattern;
}
