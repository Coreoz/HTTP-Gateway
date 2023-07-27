package com.coreoz.http.router.data;

import lombok.*;

@Value
public class DestinationRoute<T> {
    T endpointData;
    String destinationUrl;
}
