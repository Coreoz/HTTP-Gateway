package com.coreoz.http.mock;

import com.google.common.net.HttpHeaders;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

public class LocalHttpClient {
    public static final HttpClient httpClient = HttpClient.newHttpClient();

    public static HttpResponse<String> makeHttpGetRequest(int localPort, String path) {
        return makeHttpRequest(localPort, path, HttpRequest.Builder::GET);
    }

    @SneakyThrows
    public static HttpResponse<String> makeHttpRequest(int localPort, String path, Function<HttpRequest.Builder, HttpRequest.Builder> with) {
        return httpClient.send(
            with.apply(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + localPort + path)))
                .header(HttpHeaders.ACCEPT, "custom_accept")
                .header(HttpHeaders.AUTHORIZATION, "custom_auth")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
