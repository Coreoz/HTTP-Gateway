package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class HttpGatewayConfigRemoteServicesTest {
    private static final Config config = ConfigFactory.load("test.conf");

    @Test
    public void readRemoteServices__verify_all_fields_are_read_correctly() {
        List<HttpGatewayRemoteService> remoteServices = HttpGatewayConfigRemoteServices.readRemoteServices(config.getConfig("remote-services-ok"));
        Assertions
            .assertThat(remoteServices)
            .isNotEmpty()
            .hasSize(1);
        HttpGatewayRemoteService serviceTest = remoteServices.get(0);
        Assertions.assertThat(serviceTest.getServiceId()).isEqualTo("test-service");
        Assertions.assertThat(serviceTest.getBaseUrl()).isEqualTo("http://localhost:45678");
        Assertions.assertThat(serviceTest.getRoutes()).containsExactly(
            new HttpGatewayRemoteServiceRoute("fetch-pets", "GET", "/pets"),
            new HttpGatewayRemoteServiceRoute("fetch-pet", "GET", "/pets/{id}")
        );
    }

    @Test(expected = ConfigException.class)
    public void readRemoteServices__verify_that_missing_url_throws_config_exception() {
        HttpGatewayConfigRemoteServices.readRemoteServices(config.getConfig("remote-services-missing-base-url"));
    }

    @Test(expected = HttpGatewayConfigException.class)
    public void readRemoteServices__verify_that_route_path_not_starting_with_slash_throws_gateway_config_exception() {
        HttpGatewayConfigRemoteServices.readRemoteServices(config.getConfig("remote-services-wrong-route-path"));
    }

    // TODO test readRewriteRoutes and verify that routeIds must exist
}
