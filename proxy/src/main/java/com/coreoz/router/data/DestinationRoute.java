package com.coreoz.router.data;

import lombok.*;

@Value
public class DestinationRoute<T> {
    T endpointData;
    String destinationUrl;
}
