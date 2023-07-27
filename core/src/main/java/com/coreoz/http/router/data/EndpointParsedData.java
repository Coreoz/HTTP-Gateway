package com.coreoz.http.router.data;

import lombok.*;

import java.util.List;
import java.util.Map;

@Value
public class EndpointParsedData<T> {
    Map<String, Integer> patterns;
    List<ParsedSegment> destinationRouteSegments;
    HttpEndpoint<T> httpEndpoint;
}
