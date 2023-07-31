package com.coreoz.http.router.data;

import lombok.*;

@Value(staticConstructor = "of")
public class HttpEndpoint<T> {
    // TODO replace by routeId and remove generics
    T endpointData;
	String method;
	String localPath;
	String destinationPath;
	String destinationBaseUrl;
}
