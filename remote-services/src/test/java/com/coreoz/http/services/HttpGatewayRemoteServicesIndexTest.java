package com.coreoz.http.services;

import com.coreoz.http.router.data.IndexedEndpoints;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.coreoz.http.mock.ConfigExceptionValidation.validateConfigException;

import java.util.List;
import java.util.Map;

public class HttpGatewayRemoteServicesIndexTest {
    @Test
    public void computeValidatedIndexedRoutes__verify_that_correct_configuration_without_rewrite_routes_is_validated_correctly() {
        Map<String, IndexedEndpoints> routesIndex = makeIndex(baseRoutesSet(), List.of()).computeValidatedIndexedRoutes();
        Assertions.assertThat(routesIndex).hasSize(2);
        Assertions.assertThat(routesIndex.get("GET").getSegments()).hasSize(2);
        Assertions.assertThat(routesIndex.get("GET").getSegments().get("test")).isNotNull();
    }

    @Test
    public void computeValidatedIndexedRoutes__verify_that_correct_configuration_with_rewrite_routes_is_validated_correctly() {
        Map<String, IndexedEndpoints> routesIndex = makeIndex(
                baseRoutesSet(),
                List.of(new HttpGatewayRewriteRoute("route-c", "/new-route/{id}"))
            )
            .computeValidatedIndexedRoutes();
        Assertions.assertThat(routesIndex).hasSize(2);
        Assertions.assertThat(routesIndex.get("GET").getSegments()).hasSize(3);
        Assertions.assertThat(routesIndex.get("GET").getSegments().get("new-route")).isNotNull();
    }

    @Test
    public void computeValidatedIndexedRoutes__verify_that_route_path_not_starting_with_slash_throws_gateway_config_exception() {
        validateConfigException(
            () -> makeIndex(
                    List.of(new HttpGatewayRemoteServiceRoute("route-a", "GET", "wrong-path")),
                    List.of()
                )
                .computeValidatedIndexedRoutes(),
            "Downstream path 'wrong-path' must start with a / for route 'route-a'"
        );
    }

    @Test
    public void computeValidatedIndexedRoutes__verify_that_unrecognized_rewrite_route_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    baseRoutesSet(),
                    List.of(new HttpGatewayRewriteRoute("non-existing-route", "/new-route/{id}"))
                )
                .computeValidatedIndexedRoutes(),
            "Incorrect rewrite route that references a non existing routeId: non-existing-route"
        );
    }

    @Test
    public void computeValidatedIndexedRoutes__verify_that_rewrite_route_with_less_arguments_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    baseRoutesSet(),
                    List.of(new HttpGatewayRewriteRoute("route-c", "/not-enough-arg-route"))
                )
                .computeValidatedIndexedRoutes(),
            "Incorrect rewrite route for routeId='route-c'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_rewrite_route_with_more_arguments_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    baseRoutesSet(),
                    List.of(new HttpGatewayRewriteRoute("route-c", "/too-many-args-route/{id}/{other}"))
                )
                .computeValidatedIndexedRoutes(),
            "Incorrect rewrite route for routeId='route-c'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_rewrite_route_with_wrong_name_argument_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    baseRoutesSet(),
                    List.of(new HttpGatewayRewriteRoute("route-c", "/new-route/{wrong-arg-name}"))
                )
                .computeValidatedIndexedRoutes(),
            "Incorrect rewrite route for routeId='route-c'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_route_with_null_path_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    List.of(new HttpGatewayRemoteServiceRoute("route-a", "GET", null)),
                    List.of()
                )
                .computeValidatedIndexedRoutes(),
            "Downstream path cannot be null for route 'route-a'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_route_with_existing_route_id_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    List.of(
                        new HttpGatewayRemoteServiceRoute("route-a", "GET", "/test1"),
                        new HttpGatewayRemoteServiceRoute("route-a", "POST", "/test2")
                    ),
                    List.of()
                )
                .computeValidatedIndexedRoutes(),
            "Duplicate route-id for route 'route-a'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_route_with_existing_downstream_path_for_get_method_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    List.of(
                        new HttpGatewayRemoteServiceRoute("route-a", "GET", "/test1"),
                        new HttpGatewayRemoteServiceRoute("route-b", "GET", "/test1")
                    ),
                    List.of()
                )
                .computeValidatedIndexedRoutes(),
            "Duplicate downstream path for routes 'route-a' and 'route-b'"
        );
    }

    @Test
    public void validateRoutesConfig__verify_that_route_with_existing_downstream_path_with_args_for_get_method_throws_config_gateway_exception() {
        validateConfigException(
            () -> makeIndex(
                    List.of(
                        new HttpGatewayRemoteServiceRoute("route-a", "GET", "/test/{arg-a}/other/{arg-b}"),
                        new HttpGatewayRemoteServiceRoute("route-b", "GET", "/test/{id-a}/other/{id-b}")
                    ),
                    List.of()
                )
                .computeValidatedIndexedRoutes(),
            "Duplicate downstream path for routes 'route-a' and 'route-b'"
        );
    }

    // utils

    private HttpGatewayRemoteServicesIndex makeIndex(List<HttpGatewayRemoteServiceRoute> routes, List<HttpGatewayRewriteRoute> rewriteRoutes) {
        return new HttpGatewayRemoteServicesIndex(
            List.of(
                new HttpGatewayRemoteService("serviceA", "http://service.io", routes)
            ),
            rewriteRoutes
        );
    }

    private List<HttpGatewayRemoteServiceRoute> baseRoutesSet() {
        return List.of(
            new HttpGatewayRemoteServiceRoute("route-a", "GET", "/test"),
            new HttpGatewayRemoteServiceRoute("route-b", "POST", "/test"),
            new HttpGatewayRemoteServiceRoute("route-c", "GET", "/test/{id}"),
            new HttpGatewayRemoteServiceRoute("route-d", "GET", "/test/{id}/other/{other-id}"),
            new HttpGatewayRemoteServiceRoute("route-e", "GET", "/test/{id}/other/{other-id}/{id}"),
            new HttpGatewayRemoteServiceRoute("route-f", "GET", "/other"),
            new HttpGatewayRemoteServiceRoute("route-g", "GET", "/test/specific-route")
        );
    }
}
