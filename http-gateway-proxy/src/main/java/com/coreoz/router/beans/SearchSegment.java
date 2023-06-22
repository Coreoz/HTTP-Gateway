package com.coreoz.router.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayDeque;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SearchSegment<T> {
    private IndexedEndpoints<T> indexedEndpoints;
    private ArrayDeque<String> requestRemainingSegments;
    private Map<Integer, String> params;
}
