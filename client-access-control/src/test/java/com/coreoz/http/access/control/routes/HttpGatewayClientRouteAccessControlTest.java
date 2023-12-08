package com.coreoz.http.access.control.routes;

import com.coreoz.http.remoteservices.HttpGatewayRemoteService;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServiceRoute;
import com.coreoz.http.remoteservices.HttpGatewayRemoteServicesIndex;
import com.google.common.base.MoreObjects;
import org.junit.Test;

import java.util.List;

import static com.coreoz.http.mock.ConfigExceptionValidation.validateConfigException;

public class HttpGatewayClientRouteAccessControlTest {
    @Test
    public void constructor__verify_that_correct_config_passes() {
        new HttpGatewayClientRouteAccessControl(
            makeRoutesGroup(),
            makeClientRoutesControls(null, List.of("route-group-a"), null)
        );
    }

    @Test
    public void constructor__verify_that_route_group_referencing_unknown_route_throws_config_exception() {
        validateConfigException(
            () -> new HttpGatewayClientRouteAccessControl(
                makeRoutesGroup(),
                makeClientRoutesControls(null, List.of("route-group-unknown"), null)
            ),
            "Route group 'route-group-unknown' does not exist in available routes groups: [route-group-a, route-group-b]"
        );
    }

    @Test
    public void validateConfig__verify_that_valid_empty_config_passes() {
        new HttpGatewayClientRouteAccessControl(
            List.of(),
            makeClientRoutesControls(null, null, null)
        ).validateConfig(makeServiceIndex());
    }

    @Test
    public void validateConfig__verify_that_valid_allowed_routes_and_services_and_route_groups_passes() {
        new HttpGatewayClientRouteAccessControl(
            makeRoutesGroup(),
            makeClientRoutesControls(List.of("route-d"), List.of("route-group-a"), List.of("service-a"))
        ).validateConfig(makeServiceIndex());
    }

    @Test
    public void validateConfig__verify_that_unknown_route_restriction_throws_config_exception() {
        validateConfigException(
            () -> new HttpGatewayClientRouteAccessControl(
                makeRoutesGroup(),
                makeClientRoutesControls(List.of("route-unknown"), null, null)
            ).validateConfig(makeServiceIndex()),
            "Route ID 'route-unknown' is not recognized in client ID 'client-a'"
        );
    }

    @Test
    public void validateConfig__verify_that_unknown_service_restriction_throws_config_exception() {
        validateConfigException(
            () -> new HttpGatewayClientRouteAccessControl(
                makeRoutesGroup(),
                makeClientRoutesControls(null, null, List.of("service-unknown"))
            ).validateConfig(makeServiceIndex()),
            "Service ID 'service-unknown' is not recognized in client ID 'client-a'. Available service ID: [service-a]"
        );
    }

    // Utils

    private List<HttpGatewayRoutesGroup> makeRoutesGroup() {
        return List.of(
            new HttpGatewayRoutesGroup("route-group-a", List.of("route-a", "route-b")),
            new HttpGatewayRoutesGroup("route-group-b", List.of("route-c"))
        );
    }

    private List<HttpGatewayClientRoutesControl> makeClientRoutesControls(List<String> allowedRoutes, List<String> allowedRoutesGroups, List<String> allowedServices) {
        return List.of(new HttpGatewayClientRoutesControl(
            "client-a",
            MoreObjects.firstNonNull(allowedRoutes, List.of()),
            MoreObjects.firstNonNull(allowedRoutesGroups, List.of()),
            MoreObjects.firstNonNull(allowedServices, List.of())
        ));
    }

    private HttpGatewayRemoteServicesIndex makeServiceIndex() {
        return new HttpGatewayRemoteServicesIndex(
            List.of(
                new HttpGatewayRemoteService("service-a", "http://service.io", baseRoutesSet())
            ),
            List.of()
        );
    }

    private List<HttpGatewayRemoteServiceRoute> baseRoutesSet() {
        return List.of(
            new HttpGatewayRemoteServiceRoute("route-a", "GET", "/test"),
            new HttpGatewayRemoteServiceRoute("route-b", "POST", "/test"),
            new HttpGatewayRemoteServiceRoute("route-c", "GET", "/test/{id}"),
            new HttpGatewayRemoteServiceRoute("route-d", "GET", "/test/{id}/other/{other-id}")
        );
    }
}
