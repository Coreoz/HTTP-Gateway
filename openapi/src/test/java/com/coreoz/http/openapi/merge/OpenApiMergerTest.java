package com.coreoz.http.openapi.merge;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.routes.HttpRoutesValidator;
import com.coreoz.http.router.routes.ParsedRoute;
import com.google.common.io.Resources;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

// TODO more unit testing
public class OpenApiMergerTest {
    @SneakyThrows
    @Test
    public void basic_test() {
        OpenAPI baseDefinitions = readResource("/base.yaml");
        OpenAPI petStoreDefinitions = readResource("/petstore.yaml");
        OpenAPI result = OpenApiMerger.addDefinitions(
            baseDefinitions,
            petStoreDefinitions,
            new OpenApiMergerConfiguration(
                List.of(
                    new HttpEndpoint("route-test", "GET", "/pets-gateway/{id}", "/pets/{id}"),
                    new HttpEndpoint("pets", "GET", "/pets", "/pets")
                ),
                "Test",
                "test",
                true
            )
        );
        System.out.println(Yaml.mapper().writeValueAsString(result));
    }

    @SneakyThrows
    private OpenAPI readResource(String resourcePath) {
        //noinspection DataFlowIssue
        return new OpenAPIParser()
            .readContents(Resources.toString(OpenApiMergerTest.class.getResource(resourcePath), StandardCharsets.UTF_8), null, null)
            .getOpenAPI();
    }

    @Test
    public void indexEndpointsToAdd__verify_that_route_is_configured_with_the_correct_data() {
        HttpEndpoint httpEndpoint = new HttpEndpoint("a", "GET", "/test-downstream", "/test-upstream");
        @NotNull HttpRoutesValidator<HttpEndpoint> index = OpenApiMerger.indexEndpointsToAdd(List.of(httpEndpoint));
        @Nullable ParsedRoute<HttpEndpoint> route = index.findRoute("/test-upstream", "GET");
        Assertions.assertThat(route).isNotNull();
        Assertions.assertThat(route.attachedData()).isEqualTo(httpEndpoint);
    }
}
