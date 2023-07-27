package com.coreoz.http.router.data;

import lombok.*;

import java.util.Map;

@Value
public class MatchingRoute<T> {
    EndpointParsedData<T> matchingEndpoint;
    Map<Integer, String> params;
}
