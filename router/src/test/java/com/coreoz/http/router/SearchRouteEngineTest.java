package com.coreoz.http.router;

import com.coreoz.http.router.data.IndexedEndpoints;
import com.coreoz.http.router.data.MatchingRoute;
import com.coreoz.http.router.data.DestinationRoute;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

public class SearchRouteEngineTest {

    @Test
    public void searchGatewayRoute___check_that_if_no_corresponding_route_returns_empty() {
        Map<String, IndexedEndpoints> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        Optional<MatchingRoute> resultRoute = SearchRouteEngine.searchRoute(indexedEndPoints.get("GET"), "/ddddddd");
        Assertions.assertThat(resultRoute).isEmpty();
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_without_param_correct() {
        Map<String, IndexedEndpoints> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        DestinationRoute resultRoute = SearchRouteEngine.searchRoute(indexedEndPoints.get("GET"), "/test/chose").map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null)).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getDestinationUrl()).isEqualTo("/test/chose");
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_with_path_param_correct() {
        Map<String, IndexedEndpoints> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route : /test/{truc}/{bidule}
        DestinationRoute resultRoute = SearchRouteEngine.searchRoute(indexedEndPoints.get("GET"), "/test/param/machin").map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null)).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getDestinationUrl()).isEqualTo("/test/param/machin");
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_with_path_param_wrong_order_correct() {
        Map<String, IndexedEndpoints> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route :/test/{truc}/machin/{chose}
        //provider route : /test/{chose}/machin/{truc}
        DestinationRoute resultRoute =SearchRouteEngine.searchRoute(indexedEndPoints.get("GET"), "/test/bidule/machin/aaaa").map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null)).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getDestinationUrl()).isEqualTo("/test/aaaa/machin/bidule");
    }

    @Test
    public void searchGatewayRoute___check_that_returns_fail_if_one_parameter_missing() {
        Map<String, IndexedEndpoints> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route : /test/{truc}/{bidule}
        DestinationRoute resultRoute = SearchRouteEngine.searchRoute(indexedEndPoints.get("GET"), "/test/param")
            .map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null))
            .orElse(null);

        Assertions.assertThat(resultRoute).isNull();
    }

    @Test
    public void searchGatewayRoute__check_that_route_with_exact_name_matches() {
        Map<String, IndexedEndpoints> indexedEndPoint = SearchRouteIndexer.indexEndpoints(PathParamsTestsResources.endpointsTest());
        DestinationRoute resultRoute = SearchRouteEngine.searchRoute(indexedEndPoint.get("PUT"), "/test/machinchouette")
            .map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null))
            .orElse(null);
        DestinationRoute resultRoute2 = SearchRouteEngine.searchRoute(indexedEndPoint.get("PUT"), "/test/chouette")
            .map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null))
            .orElse(null);

        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute2).isNotNull();
        Assertions.assertThat(resultRoute.getDestinationUrl()).isEqualTo("/test/machinchouette-found");
        Assertions.assertThat(resultRoute2.getDestinationUrl()).isEqualTo("/test/chouette-found");
    }

    @Test
    public void searchGatewayRoute__check_that_route_with_non_exact_name_matches() {
        Map<String, IndexedEndpoints> indexedEndPoint = SearchRouteIndexer.indexEndpoints(PathParamsTestsResources.endpointsTest());
        DestinationRoute resultRoute = SearchRouteEngine.searchRoute(indexedEndPoint.get("PUT"), "/test/wildcard-route")
            .map(matchingRoute -> SearchRouteEngine.computeDestinationRoute(matchingRoute, null))
            .orElse(null);

        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getDestinationUrl()).isEqualTo("/test/wildcard-route");
    }
}
