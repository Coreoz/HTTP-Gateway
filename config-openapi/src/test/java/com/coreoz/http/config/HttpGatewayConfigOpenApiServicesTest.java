package com.coreoz.http.config;

import com.coreoz.http.mock.ConfigExceptionValidation;
import com.coreoz.http.openapi.fetching.OpenApiFetcher;
import com.coreoz.http.openapi.fetching.http.OpenApiHttpFetcher;
import com.coreoz.http.openapi.fetching.resources.OpenApiResourceFetcher;
import com.coreoz.http.upstreamauth.HttpGatewayRemoteServiceBasicAuthenticator;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigOpenApiServicesTest {
    static final Config testConfig = ConfigFactory.load();

    @Test
    public void readConfig__verify_that_config_can_be_read_successfully_and_empty_openapi_service_is_ignored() {
        List<OpenApiFetcher> openApiFetchers = HttpGatewayConfigOpenApiServices.readConfig(
            new HttpGatewayConfigOpenApiServicesParameters(testConfig.getConfig("basic-test"))
        );
        AbstractObjectAssert<?, ?> fetcherConfigurationAssert = Assertions.assertThat(openApiFetchers)
            // verify that the openapi config has been read for service test-service
            // and verify that for the other-service service, the openapi config is skipped (since it is absent)
            .hasSize(1)
            .first()
            .isInstanceOf(OpenApiHttpFetcher.class)
            .extracting("configuration");
        fetcherConfigurationAssert
            .extracting("serviceId", "remoteUrl").isEqualTo(List.of("test-service", "http://localhost:4567/swagger"));
        fetcherConfigurationAssert.extracting("upstreamAuthenticator").isInstanceOf(HttpGatewayRemoteServiceBasicAuthenticator.class);
    }

    @Test
    public void readConfig__verify_that_unsupported_openapi_type_throws_HttpGatewayValidationException() {
        ConfigExceptionValidation.validateConfigException(() -> HttpGatewayConfigOpenApiServices.readConfig(
            new HttpGatewayConfigOpenApiServicesParameters(testConfig.getConfig("wrong-openapi-type"))
        ), "Unrecognized OpenAPI type  'unrecognized' for service test-service");
    }

    @Test
    public void readConfig__verify_that_openapi_resource_config_can_be_read_successfully() {
        List<OpenApiFetcher> openApiFetchers = HttpGatewayConfigOpenApiServices.readConfig(
            new HttpGatewayConfigOpenApiServicesParameters(testConfig.getConfig("resource-fetcher"))
        );
        Assertions.assertThat(openApiFetchers)
            .hasSize(1)
            .first()
            .isInstanceOf(OpenApiResourceFetcher.class)
            .extracting("resourcePath").isEqualTo("/openapi/test-service.yml");
    }

    @Test
    public void readConfig__verify_that_openapi_remote_url_config_can_be_read_successfully() {
        List<OpenApiFetcher> openApiFetchers = HttpGatewayConfigOpenApiServices.readConfig(
            new HttpGatewayConfigOpenApiServicesParameters(testConfig.getConfig("remote-url-fetcher"))
        );
        Assertions.assertThat(openApiFetchers)
            .hasSize(1)
            .first()
            .isInstanceOf(OpenApiHttpFetcher.class)
            .extracting("configuration")
            .extracting("remoteUrl").isEqualTo("https://openapi.test/definition");
    }
}
