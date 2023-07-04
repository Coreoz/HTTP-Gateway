package com.coreoz.router.beans;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
@ToString
public class EndpointParsedData<T> {
    private Map<String, Integer> patterns;
    private List<ParsedSegment> destinationRouteSegments;
    private HttpEndpoint<T> httpEndpoint;
}
