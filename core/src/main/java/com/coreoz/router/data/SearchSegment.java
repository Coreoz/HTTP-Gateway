package com.coreoz.router.data;

import lombok.Value;

import java.util.ArrayDeque;
import java.util.Map;

@Value
public class SearchSegment<T> {
    IndexedEndpoints<T> indexedEndpoints;
    ArrayDeque<String> requestRemainingSegments;
    Map<Integer, String> params;
}
