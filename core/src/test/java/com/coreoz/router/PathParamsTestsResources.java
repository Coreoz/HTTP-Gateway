package com.coreoz.router;

import com.coreoz.router.data.HttpEndpoint;
import com.coreoz.router.data.EndpointParsedData;
import com.coreoz.router.data.IndexedEndpoints;
import com.coreoz.router.data.ParsedSegment;

import java.util.List;
import java.util.Map;

public class PathParamsTestsResources {

    public static List<HttpEndpoint<Long>> endpointsTest() {
        return endpointsTest;
    }

    private static final List<HttpEndpoint<Long>> endpointsTest = List.of(
            HttpEndpoint.of(1L, "GET", "/test/chose", "/test/chose", ""),
            HttpEndpoint.of(2L, "GET", "/test/bidule/chose", "/test/bidule/chose", ""),
            HttpEndpoint.of(3L, "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", ""),
            HttpEndpoint.of(4L, "GET", "/test/{truc}/machin", "/test/{truc}/machin", ""),
            HttpEndpoint.of(5L, "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", ""),
            HttpEndpoint.of(6L, "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", ""),
            HttpEndpoint.of(7L, "PUT", "/test/chouette", "/test/chouette-found", ""),
            HttpEndpoint.of(8L, "PUT", "/test/{truc}", "/test/{truc}", ""),
            HttpEndpoint.of(9L, "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
    );

    static IndexedEndpoints<Long> choseSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", false)),
                    HttpEndpoint.of(1L, "GET", "/test/chose", "/test/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testBiduleChoseSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("bidule", false), new ParsedSegment("chose", false)),
                    HttpEndpoint.of(2L, "GET", "/test/bidule/chose", "/test/bidule/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60 | 1L << 59,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testTrucBidulePattern = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of("truc", 2, "bidule",3),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("bidule", true)),
                    HttpEndpoint.of(3L, "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", "")
            ),
            1L << 62 | 1L << 61,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> testTrucMachinTrucSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false), new ParsedSegment("truc", false)),
                    HttpEndpoint.of(6L, "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59 | 1L << 58,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints<Long> testTrucMachinChosePattern = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of("truc", 2, "chose", 4),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", true), new ParsedSegment("machin", false), new ParsedSegment("truc", true)),
                    HttpEndpoint.of(5L, "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints<Long> testTrucMachinSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false)),
                    HttpEndpoint.of(4L, "GET", "/test/{truc}/machin", "/test/{truc}/machin", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            3,
            Map.of("truc", testTrucMachinTrucSegment),
            testTrucMachinChosePattern
    );

    static IndexedEndpoints<Long> biduleSegments = new IndexedEndpoints<>(
            null,
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of("chose", testBiduleChoseSegment),
            null
    );

    static IndexedEndpoints<Long> testPattern = new IndexedEndpoints<>(
            null,
            1L << 62 | 1L << 61,
            2,
            Map.of("machin", testTrucMachinSegment),
            testTrucBidulePattern
    );

    static IndexedEndpoints<Long> testSegments = new IndexedEndpoints<>(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("bidule", biduleSegments, "chose", choseSegment),
            testPattern
    );

    static IndexedEndpoints<Long> putTestPattern = new IndexedEndpoints<>(
             new EndpointParsedData<>(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true)),
                    HttpEndpoint.of(8L, "PUT", "/test/{truc}", "/test/{truc}", "")
            ),
            1L << 62 | 1L << 61,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putChouetteSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chouette-found", false)),
                    HttpEndpoint.of(7L, "PUT", "/test/chouette", "/test/chouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putMachinChouetteSegment = new IndexedEndpoints<>(
            new EndpointParsedData<>(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("machinchouette-found", false)),
                    HttpEndpoint.of(9L, "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints<Long> putTestSegment = new IndexedEndpoints<>(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("machinchouette", putMachinChouetteSegment, "chouette", putChouetteSegment),
            putTestPattern
    );

    static Map<String, IndexedEndpoints<Long>> indexedEndpointsResult = Map.of(
            "GET", new IndexedEndpoints<>(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", testSegments),
                    null
            ),
            "PUT", new IndexedEndpoints<>(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", putTestSegment),
                    null
            )
    );
}
