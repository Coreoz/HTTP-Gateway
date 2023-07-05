package com.coreoz.router.data;

import lombok.*;

@Getter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class HttpEndpoint<T> {
    private T endpointData;
	private String method;
	private String localPath;
	private String destinationPath;
	private String destinationBaseUrl;
}
