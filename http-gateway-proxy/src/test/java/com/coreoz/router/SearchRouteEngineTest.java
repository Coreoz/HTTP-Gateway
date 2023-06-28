package com.coreoz.router;

import com.coreoz.router.beans.IndexedEndpoints;
import com.coreoz.router.beans.MatchingRoute;
import com.coreoz.router.beans.TargetRoute;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

public class SearchRouteEngineTest {

    @Test
    public void searchGatewayRoute___check_that_if_no_corresponding_route_returns_empty() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        Optional<MatchingRoute<Long>> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoints.get("GET"), "/ddddddd");
        Assertions.assertThat(resultRoute).isEmpty();
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_without_param_correct() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        TargetRoute<Long> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoints.get("GET"), "/test/chose").map(SearchRouteEngine::toProviderRoute).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getTargetUrl()).isEqualTo("/test/chose");
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_with_path_param_correct() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route : /test/{truc}/{bidule}
        TargetRoute<Long> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoints.get("GET"), "/test/param/machin").map(SearchRouteEngine::toProviderRoute).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getTargetUrl()).isEqualTo("/test/param/machin");
    }

    @Test
    public void searchGatewayRoute___check_that_mapping_with_path_param_wrong_order_correct() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route :/test/{truc}/machin/{chose}
        //provider route : /test/{chose}/machin/{truc}
        TargetRoute<Long> resultRoute =SearchRouteEngine.searchGatewayRoute(indexedEndPoints.get("GET"), "/test/bidule/machin/aaaa").map(SearchRouteEngine::toProviderRoute).orElse(null);
        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getTargetUrl()).isEqualTo("/test/aaaa/machin/bidule");
    }

    @Test
    public void searchGatewayRoute___check_that_returns_fail_if_one_parameter_missing() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoints = PathParamsTestsResources.indexedEndpointsResult;
        // gateway route : /test/{truc}/{bidule}
        TargetRoute<Long> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoints.get("GET"), "/test/param")
            .map(SearchRouteEngine::toProviderRoute)
            .orElse(null);

        Assertions.assertThat(resultRoute).isNull();
    }

    @Test
    public void searchGatewayRoute__check_that_route_with_exact_name_matches() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoint = SearchRouteIndexer.indexEndpoints(PathParamsTestsResources.endpointsTest());
        TargetRoute<Long> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoint.get("PUT"), "/test/machinchouette")
            .map(SearchRouteEngine::toProviderRoute)
            .orElse(null);
        TargetRoute<Long> resultRoute2 = SearchRouteEngine.searchGatewayRoute(indexedEndPoint.get("PUT"), "/test/chouette")
            .map(SearchRouteEngine::toProviderRoute)
            .orElse(null);

        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute2).isNotNull();
        Assertions.assertThat(resultRoute.getTargetUrl()).isEqualTo("/test/machinchouette-found");
        Assertions.assertThat(resultRoute2.getTargetUrl()).isEqualTo("/test/chouette-found");
    }

    @Test
    public void searchGatewayRoute__check_that_route_with_non_exact_name_matches() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoint = SearchRouteIndexer.indexEndpoints(PathParamsTestsResources.endpointsTest());
        TargetRoute<Long> resultRoute = SearchRouteEngine.searchGatewayRoute(indexedEndPoint.get("PUT"), "/test/wildcard-route")
            .map(SearchRouteEngine::toProviderRoute)
            .orElse(null);

        Assertions.assertThat(resultRoute).isNotNull();
        Assertions.assertThat(resultRoute.getTargetUrl()).isEqualTo("/test/wildcard-route");
    }
}
