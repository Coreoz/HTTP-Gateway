package com.coreoz.mock;

import io.netty.handler.codec.http.HttpResponseStatus;
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
        Spark.get("/echo/:param", (request, response) -> request.params("param"));
        Spark.get("/slow-api", (request, response) -> {
            Thread.sleep(300);
            return "slow response";
        });
        Spark.post("/no-body-simple", (request, response) -> {
            response.status(HttpResponseStatus.CREATED.code());
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
