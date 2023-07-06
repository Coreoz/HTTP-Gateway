package com.coreoz.router.data;

import lombok.*;

@Value(staticConstructor = "of")
public class HttpEndpoint<T> {
    T endpointData;
	String method;
	String localPath;
	String destinationPath;
	String destinationBaseUrl;
}
