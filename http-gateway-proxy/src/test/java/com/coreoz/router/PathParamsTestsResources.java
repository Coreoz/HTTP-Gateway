package com.coreoz.router;

import com.coreoz.router.beans.ApiEndpoint;
import com.coreoz.router.beans.EndpointParsedData;
import com.coreoz.router.beans.IndexedEndpoints;
import com.coreoz.router.beans.ParsedSegment;

import java.util.List;
import java.util.Map;

public class PathParamsTestsResources {

    public static List<ApiEndpoint<Long>> endpointsTest() {
        return endpointsTest;
    }

    private static final List<ApiEndpoint<Long>> endpointsTest = List.of(
            ApiEndpoint.of(1L, "GET", "/test/chose", "/test/chose", ""),
            ApiEndpoint.of(2L, "GET", "/test/bidule/chose", "/test/bidule/chose", ""),
            ApiEndpoint.of(3L, "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", ""),
            ApiEndpoint.of(4L, "GET", "/test/{truc}/machin", "/test/{truc}/machin", ""),
            ApiEndpoint.of(5L, "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", ""),
            ApiEndpoint.of(6L, "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", ""),
            ApiEndpoint.of(7L, "PUT", "/test/chouette", "/test/chouette-found", ""),
            ApiEndpoint.of(8L, "PUT", "/test/{truc}", "/test/{truc}", ""),
            ApiEndpoint.of(9L, "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
    );

    static IndexedEndpoints<Long> choseSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", false)),
                    ApiEndpoint.of(1L, "GET", "/test/chose", "/test/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testBiduleChoseSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("bidule", false), new ParsedSegment("chose", false)),
                    ApiEndpoint.of(2L, "GET", "/test/bidule/chose", "/test/bidule/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60 | 1L << 59,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testTrucBidulePattern = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of("truc", 2, "bidule",3),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("bidule", true)),
                    ApiEndpoint.of(3L, "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", "")
            ),
            1L << 62 | 1L << 61,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testTrucMachinTrucSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false), new ParsedSegment("truc", false)),
                    ApiEndpoint.of(6L, "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59 | 1L << 58,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints<Long> testTrucMachinChosePattern = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of("truc", 2, "chose", 4),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", true), new ParsedSegment("machin", false), new ParsedSegment("truc", true)),
                    ApiEndpoint.of(5L, "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints<Long> testTrucMachinSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false)),
                    ApiEndpoint.of(4L, "GET", "/test/{truc}/machin", "/test/{truc}/machin", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            3,
            Map.of("truc", testTrucMachinTrucSegment),
            testTrucMachinChosePattern
    );

    static IndexedEndpoints<Long> biduleSegments = IndexedEndpoints.of(
            null,
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of("chose", testBiduleChoseSegment),
            null
    );

    static IndexedEndpoints<Long> testPattern = IndexedEndpoints.of(
            null,
            1L << 62 | 1L << 61,
            2,
            Map.of("machin", testTrucMachinSegment),
            testTrucBidulePattern
    );

    static IndexedEndpoints<Long> testSegments = IndexedEndpoints.of(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("bidule", biduleSegments, "chose", choseSegment),
            testPattern
    );

    static IndexedEndpoints<Long> putTestPattern = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true)),
                    ApiEndpoint.of(8L, "PUT", "/test/{truc}", "/test/{truc}", "")
            ),
            1L << 62 | 1L << 61,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putChouetteSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chouette-found", false)),
                    ApiEndpoint.of(7L, "PUT", "/test/chouette", "/test/chouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putMachinChouetteSegment = IndexedEndpoints.of(
            EndpointParsedData.of(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("machinchouette-found", false)),
                    ApiEndpoint.of(9L, "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putTestSegment = IndexedEndpoints.of(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("machinchouette", putMachinChouetteSegment, "chouette", putChouetteSegment),
            putTestPattern
    );

    static Map<String, IndexedEndpoints<Long>> indexedEndpointsResult = Map.of(
            "GET", IndexedEndpoints.of(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", testSegments),
                    null
            ),
            "PUT", IndexedEndpoints.of(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", putTestSegment),
                    null
            )
    );
}
