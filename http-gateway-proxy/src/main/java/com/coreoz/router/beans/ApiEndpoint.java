package com.coreoz.router.beans;

import lombok.*;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ApiEndpoint<T> {
    private T endpointData;
	private String method;
	private String gatewayPath;
	private String providerPath;
	private String providerBaseUrl;
}
