package com.coreoz.router.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class MatchingRoute<T> {
    private EndpointParsedData<T> matchingEndpoint;
    private Map<Integer, String> params;
}
