package com.coreoz.http.router.data;

import lombok.Value;

import java.util.ArrayDeque;
import java.util.Map;

@Value
public class SearchSegment {
    IndexedEndpoints indexedEndpoints;
    ArrayDeque<String> requestRemainingSegments;
    Map<Integer, String> params;
}
