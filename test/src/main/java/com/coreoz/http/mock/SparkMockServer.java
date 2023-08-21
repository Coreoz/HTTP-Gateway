package com.coreoz.http.mock;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import spark.Spark;

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
