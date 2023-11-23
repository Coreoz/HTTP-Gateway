package com.coreoz.http.mock;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import spark.Spark;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SparkMockServer {
    public static int SPARK_HTTP_PORT = 4567;

    private static volatile boolean isInitialized = false;

    public static synchronized void initialize() {
        if (!isInitialized) {
            initializeSpark();
            isInitialized = true;
        }
    }

    private static void initializeSpark() {
        Spark.port(SPARK_HTTP_PORT);
        Spark.get("/hello", (request, response) -> "World");
        Spark.get("/pets", (request, response) -> {
            String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString("test-auth:auth-password".getBytes(StandardCharsets.UTF_8));
            if (!request.headers(HttpHeaders.AUTHORIZATION).equals(basicAuthHeader)) {
                return "wrong auth";
            }
            // custom header for testing
            response.header("X-Tenants", request.headers("X-Tenants"));

            return "Lots of pets :)";
        });
        Spark.get("/customer-a/other-route/:id", (request, response) -> "Customer A: " + request.params("id"));
        Spark.get("/customer-a/custom-route", (request, response) -> "Customer A custom route");
        Spark.get("/customer-b/other-route/:id", (request, response) -> "Customer B: " + request.params("id"));
        Spark.get("/echo/:param", (request, response) -> request.params("param")
            + "\n"
            + request.queryString()
            + "\n"
            + "accept-header=" + request.headers(HttpHeaders.ACCEPT)
            + "\n"
            + "authorization=" + request.headers(HttpHeaders.AUTHORIZATION));
        Spark.get("/slow-api", (request, response) -> {
            Thread.sleep(300);
            return "slow response";
        });
        Spark.post("/no-body-simple", (request, response) -> {
            response.status(HttpStatus.CREATED_201);
            // make sure no body is returned
            response.raw().getOutputStream().close();
            // spark does not allow null responses...
            return "";
        });
        Spark.get("/long-body", (request, response) -> {
            response.header("Sample-Response-Header", "Sample response value");
            return "This is a loooooooooooooooooong body";
        });
        Spark.post("/long-body", (request, response) -> {
            response.header("Sample-Response-Header", "Sample response value");
            return "This is a loooooooooooooooooong body";
        });
        Spark.post("/server-error", (request, response) -> {
            response.status(500);
            return "Internal server error!";
        });
        Spark.post("/client-error", (request, response) -> {
            response.status(400);
            return "Data validation failed";
        });
    }
}
