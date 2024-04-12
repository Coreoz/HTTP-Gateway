package com.coreoz.http.openapi.fetching.resources;

import com.coreoz.http.openapi.fetching.OpenApiFetchingData;
import com.coreoz.http.openapi.fetching.OpenApiTestFetchers;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class OpenApiResourceFetcherTest {
    @Test
    public void verify_that_not_found_file_returns_null_data() {
        OpenApiFetchingData openApiFetchingData = fetchUsingOpenApiResourceFetcher("non-existent-file.yaml");
        Assertions.assertThat(openApiFetchingData).isNull();
    }

    @Test
    public void verify_that_wrong_format_file_returns_null_data() {
        OpenApiFetchingData openApiFetchingData = fetchUsingOpenApiResourceFetcher("hello.yaml");
        Assertions.assertThat(openApiFetchingData).isNull();
    }

    @Test
    public void verify_that_valid_open_api_file_returns_data() {
        OpenApiFetchingData openApiFetchingData = fetchUsingOpenApiResourceFetcher("base.yaml");
        OpenApiTestFetchers.assertOpenApiTitle(openApiFetchingData, "HTTP API Gateway sample");
    }

    @SneakyThrows
    private OpenApiFetchingData fetchUsingOpenApiResourceFetcher(String resourcePath) {
        return new OpenApiResourceFetcher("test-service", resourcePath).fetch().get();
    }
}
