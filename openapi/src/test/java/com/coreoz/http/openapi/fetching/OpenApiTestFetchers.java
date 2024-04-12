package com.coreoz.http.openapi.fetching;

import org.assertj.core.api.Assertions;

public class OpenApiTestFetchers {
    public static void assertOpenApiTitle(OpenApiFetchingData mergedOpenApi, String title) {
        Assertions.assertThat(mergedOpenApi).isNotNull();
        Assertions.assertThat(mergedOpenApi.openApiDefinition().getInfo().getTitle()).isEqualTo(title);
    }
}
