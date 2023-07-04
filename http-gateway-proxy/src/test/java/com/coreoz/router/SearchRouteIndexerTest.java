package com.coreoz.router;

import com.coreoz.router.beans.HttpEndpoint;
import com.coreoz.router.beans.EndpointParsedData;
import com.coreoz.router.beans.IndexedEndpoints;
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
        EndpointParsedData<Long> addedEndpoint = SearchRouteIndexer.addEndpointToIndex(HttpEndpoint.of(1L, "GET", "/test", "/test", ""), index);
        Assertions.assertThat(addedEndpoint).isNotNull();
        Assertions.assertThat(addedEndpoint.getHttpEndpoint().getEndpointData()).isEqualTo(1L);
        Assertions.assertThat(index.size()).isEqualTo(1);
    }

    @Test
    public void addEndpointToIndex__check_that_adding_an_existing_endpoint_returns_existing_endpoint() {
        Map<String, IndexedEndpoints<Long>> index = new HashMap<>();
        SearchRouteIndexer.addEndpointToIndex(HttpEndpoint.of(1L, "GET", "/test", "/test", ""), index);
        EndpointParsedData<Long> existingEndpoint = SearchRouteIndexer.addEndpointToIndex(HttpEndpoint.of(2L, "GET", "/test", "/test", ""), index);

        Assertions.assertThat(existingEndpoint).isNotNull();
        Assertions.assertThat(existingEndpoint.getHttpEndpoint().getEndpointData()).isEqualTo(1L);
        Assertions.assertThat(index.size()).isEqualTo(1);
    }
}
