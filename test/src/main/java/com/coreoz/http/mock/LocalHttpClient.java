package com.coreoz.http.mock;

import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LocalHttpClient {
    public static HttpResponse<String> makeHttpRequest(int localPort, String path) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + localPort + path))
                .GET()
                .header(HttpHeaders.ACCEPT, "custom_accept")
                .header(HttpHeaders.AUTHORIZATION, "custom_auth")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
