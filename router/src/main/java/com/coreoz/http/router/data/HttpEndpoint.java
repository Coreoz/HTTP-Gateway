package com.coreoz.http.router.data;

import lombok.*;

@Value
public class HttpEndpoint {
    String routeId;
	String method;
	String downstreamPath;
	String upstreamPath;
}
