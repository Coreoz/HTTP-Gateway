package com.coreoz.http.router.data;

import lombok.*;

@Value
public class HttpEndpoint {
    String routeId;
	String method;
	String localPath;
	String destinationPath;
    // TODO Remove this, it has no sense to be duplicated here
	String destinationBaseUrl;
}
