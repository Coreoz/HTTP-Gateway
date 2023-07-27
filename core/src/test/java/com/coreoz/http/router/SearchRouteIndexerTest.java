package com.coreoz.http.router;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.EndpointParsedData;
import com.coreoz.http.router.data.IndexedEndpoints;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SearchRouteIndexerTest {

    @Test
    public void indexEndpoints__check_that_indexation_function_order_is_correct() {
        Map<String, IndexedEndpoints<Long>> indexedEndPoint = SearchRouteIndexer.indexEndpoints(PathParamsTestsResources.endpointsTest());
        Assertions.assertThat(indexedEndPoint).containsExactlyInAnyOrderEntriesOf(PathParamsTestsResources.indexedEndpointsResult);
    }

    @Test
    public void addEndpointToIndex__check_that_adding_endpoint_returns_added_endpoint() {
        Map<String, IndexedEndpoints<Long>> index = new HashMap<>();
        EndpointParsedData<Long> addedEndpoint = SearchRouteIndexer.addEndpointToIndex(index, HttpEndpoint.of(1L, "GET", "/test", "/test", ""));
        Assertions.assertThat(addedEndpoint).isNotNull();
        Assertions.assertThat(addedEndpoint.getHttpEndpoint().getEndpointData()).isEqualTo(1L);
        Assertions.assertThat(index.size()).isEqualTo(1);
    }

    @Test
    public void addEndpointToIndex__check_that_adding_an_existing_endpoint_returns_existing_endpoint() {
        Map<String, IndexedEndpoints<Long>> index = new HashMap<>();
        SearchRouteIndexer.addEndpointToIndex(index, HttpEndpoint.of(1L, "GET", "/test", "/test", ""));
        EndpointParsedData<Long> existingEndpoint = SearchRouteIndexer.addEndpointToIndex(index, HttpEndpoint.of(2L, "GET", "/test", "/test", ""));

        Assertions.assertThat(existingEndpoint).isNotNull();
        Assertions.assertThat(existingEndpoint.getHttpEndpoint().getEndpointData()).isEqualTo(1L);
        Assertions.assertThat(index.size()).isEqualTo(1);
    }
}
