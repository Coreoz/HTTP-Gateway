package com.coreoz.http.router.data;

import lombok.*;

import java.util.Map;

@Value
public class MatchingRoute {
    EndpointParsedData matchingEndpoint;
    Map<Integer, String> params;
}
