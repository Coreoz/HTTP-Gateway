package com.coreoz.router.data;

import lombok.*;

@Value
public class TargetRoute<T> {
    private T endpointData;
    private String targetUrl;
}
