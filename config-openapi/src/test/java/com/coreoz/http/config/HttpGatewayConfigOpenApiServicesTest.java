package com.coreoz.http.config;

import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcher;
import com.coreoz.http.openapi.service.OpenApiUpstreamParameters;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigOpenApiServicesTest {
    @Test
    public void verify_that_config_can_be_read_successfully() {
        List<OpenApiFetcher> openApiFetchers = HttpGatewayConfigOpenApiServices.readConfig(new HttpGatewayConfigOpenApiServicesParameters(ConfigFactory.load()));
        Assertions.assertThat(openApiFetchers)
            .hasSize(1)
            .first()
            .isInstanceOf(OpenApiHttpFetcher.class)
            .extracting("configuration")
            .extracting("serviceId", "remoteUrl").isEqualTo(List.of("test-service", "http://localhost:4567/swagger"));
    }
}
