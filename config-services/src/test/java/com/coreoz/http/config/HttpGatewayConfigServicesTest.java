package com.coreoz.http.config;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.coreoz.http.remoteservices.HttpGatewayRewriteRoute;
import com.coreoz.http.router.data.HttpEndpoint;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class HttpGatewayConfigServicesTest {
    private static final Config config = ConfigFactory.load("test.conf");

    @Test
    public void readRemoteServices__verify_all_fields_are_read_correctly() {
        List<HttpGatewayRemoteService> remoteServices = HttpGatewayConfigServices.readRemoteServices(config.getConfig("remote-services-ok"));
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
        HttpGatewayConfigServices.readRemoteServices(config.getConfig("remote-services-missing-base-url"));
    }

    // readRewriteRoutes

    @Test
    public void readRewriteRoutes__verify_that_rewrite_routes_are_read_correctly() {
        List<HttpGatewayRewriteRoute> rewriteRoutes = HttpGatewayConfigServices.readRewriteRoutes(config.getConfig("gateway-rewrite-route-test"));
        Assertions.assertThat(rewriteRoutes).containsExactly(new HttpGatewayRewriteRoute("route-a", "/pets"));
    }

    @Test
    public void readRewriteRoutes__verify_that_missing_rewrite_route_config_return_empty_list() {
        List<HttpGatewayRewriteRoute> emptyRewriteRoutes = HttpGatewayConfigServices.readRewriteRoutes(config.getConfig("remote-services-missing-base-url"));
        Assertions.assertThat(emptyRewriteRoutes).isEmpty();
    }

    // readConfig

    @Test
    public void readConfig__verify_that_config_is_correctly_read() {
        HttpGatewayRemoteServicesIndex remoteServiceIndex = HttpGatewayConfigServices.readConfig(config.getConfig("remote-services-ok"));
        List<HttpEndpoint> routes = StreamSupport.stream(remoteServiceIndex.computeRoutes().spliterator(), false).collect(Collectors.toList());
        Assertions.assertThat(remoteServiceIndex.getServices()).hasSize(1);
        Assertions.assertThat(routes)
            .hasSize(2)
            .contains(new HttpEndpoint("fetch-pet", "GET", "/custom-fetch-pet/{id}", "/pets/{id}"));
    }
}
