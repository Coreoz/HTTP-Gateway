package com.coreoz.http.router;

import com.coreoz.http.router.data.HttpEndpoint;
import com.coreoz.http.router.data.EndpointParsedData;
import com.coreoz.http.router.data.IndexedEndpoints;
import com.coreoz.http.router.data.ParsedSegment;

import java.util.List;
import java.util.Map;

public class PathParamsTestsResources {

    public static List<HttpEndpoint> endpointsTest() {
        return endpointsTest;
    }

    private static final List<HttpEndpoint> endpointsTest = List.of(
            new HttpEndpoint("1", "GET", "/test/chose", "/test/chose", ""),
            new HttpEndpoint("2", "GET", "/test/bidule/chose", "/test/bidule/chose", ""),
            new HttpEndpoint("3", "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", ""),
            new HttpEndpoint("4", "GET", "/test/{truc}/machin", "/test/{truc}/machin", ""),
            new HttpEndpoint("5", "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", ""),
            new HttpEndpoint("6", "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", ""),
            new HttpEndpoint("7", "PUT", "/test/chouette", "/test/chouette-found", ""),
            new HttpEndpoint("8", "PUT", "/test/{truc}", "/test/{truc}", ""),
            new HttpEndpoint("9", "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
    );

    static IndexedEndpoints choseSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", false)),
                    new HttpEndpoint("1", "GET", "/test/chose", "/test/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints testBiduleChoseSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("bidule", false), new ParsedSegment("chose", false)),
                    new HttpEndpoint("2", "GET", "/test/bidule/chose", "/test/bidule/chose", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60 | 1L << 59,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints testTrucBidulePattern = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of("truc", 2, "bidule",3),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("bidule", true)),
                    new HttpEndpoint("3", "GET", "/test/{truc}/{bidule}", "/test/{truc}/{bidule}", "")
            ),
            1L << 62 | 1L << 61,
            3,
            Map.of(),
            null
    );

    static IndexedEndpoints testTrucMachinTrucSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false), new ParsedSegment("truc", false)),
                    new HttpEndpoint("6", "GET", "/test/{truc}/machin/truc", "/test/{truc}/machin/truc", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59 | 1L << 58,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints testTrucMachinChosePattern = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of("truc", 2, "chose", 4),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chose", true), new ParsedSegment("machin", false), new ParsedSegment("truc", true)),
                    new HttpEndpoint("5", "GET", "/test/{truc}/machin/{chose}", "/test/{chose}/machin/{truc}", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            4,
            Map.of(),
            null
    );
    static IndexedEndpoints testTrucMachinSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true), new ParsedSegment("machin", false)),
                    new HttpEndpoint("4", "GET", "/test/{truc}/machin", "/test/{truc}/machin", "")
            ),
            1L << 62 | 1L << 61 | 1L << 59,
            3,
            Map.of("truc", testTrucMachinTrucSegment),
            testTrucMachinChosePattern
    );

    static IndexedEndpoints biduleSegments = new IndexedEndpoints(
            null,
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of("chose", testBiduleChoseSegment),
            null
    );

    static IndexedEndpoints testPattern = new IndexedEndpoints(
            null,
            1L << 62 | 1L << 61,
            2,
            Map.of("machin", testTrucMachinSegment),
            testTrucBidulePattern
    );

    static IndexedEndpoints testSegments = new IndexedEndpoints(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("bidule", biduleSegments, "chose", choseSegment),
            testPattern
    );

    static IndexedEndpoints putTestPattern = new IndexedEndpoints(
             new EndpointParsedData(
                    Map.of("truc", 2),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("truc", true)),
                    new HttpEndpoint("8", "PUT", "/test/{truc}", "/test/{truc}", "")
            ),
            1L << 62 | 1L << 61,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints putChouetteSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("chouette-found", false)),
                    new HttpEndpoint("7", "PUT", "/test/chouette", "/test/chouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints putMachinChouetteSegment = new IndexedEndpoints(
            new EndpointParsedData(
                    Map.of(),
                    List.of(new ParsedSegment("test", false), new ParsedSegment("machinchouette-found", false)),
                    new HttpEndpoint("9", "PUT", "/test/machinchouette", "/test/machinchouette-found", "")
            ),
            1L << 62 | 1L << 61 | 1L << 60,
            2,
            Map.of(),
            null
    );

    static IndexedEndpoints putTestSegment = new IndexedEndpoints(
            null,
            1L << 62 | 1L << 61,
            1,
            Map.of("machinchouette", putMachinChouetteSegment, "chouette", putChouetteSegment),
            putTestPattern
    );

    static Map<String, IndexedEndpoints> indexedEndpointsResult = Map.of(
            "GET", new IndexedEndpoints(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", testSegments),
                    null
            ),
            "PUT", new IndexedEndpoints(
                    null,
                    1L << 62,
                    0,
                    Map.of("test", putTestSegment),
                    null
            )
    );
}
