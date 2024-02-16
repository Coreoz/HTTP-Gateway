package com.coreoz.http.config;

import com.coreoz.http.openapi.service.OpenApiUpstreamParameters;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigOpenApiServicesTest {
    @Test
    public void verify_that_config_can_be_read_successfully() {
        List<OpenApiUpstreamParameters> openApiServicesConfig = HttpGatewayConfigOpenApiServices.readConfig(ConfigFactory.load());
        Assertions.assertThat(openApiServicesConfig)
            .hasSize(1)
            .first()
            .extracting("serviceId", "openApiRemotePath").isEqualTo(List.of("test-service", "/swagger"));
    }
}
