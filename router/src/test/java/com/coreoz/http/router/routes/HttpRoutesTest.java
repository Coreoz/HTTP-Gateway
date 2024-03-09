package com.coreoz.http.router.routes;

import com.coreoz.http.router.data.ParsedSegment;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

public class HttpRoutesTest {
    @Test
    public void parsePathAsSegments__verify_that_an_empty_path_returns_an_empty_segments_list() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("")).isEmpty();
    }

    @Test
    public void parsePathAsSegments__verify_that_an_slash_path_returns_an_empty_segments_list() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/")).isEmpty();
    }

    @Test
    public void parsePathAsSegments__verify_that_two_following_flash_returns_an_empty_segments() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test//other")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("", false),
            new ParsedSegment("other", false)
        );
    }

    @Test
    public void parsePathAsSegments__verify_that_an_empty_pattern_returns_an_empty_pattern_segment() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test/{}/other")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("", true),
            new ParsedSegment("other", false)
        );
    }

    @Test
    public void parsePathAsSegments__verify_that_an_unclosed_start_pattern_is_parsed_as_a_regular_segment() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test/{unclosed-pattern/other")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("{unclosed-pattern", false),
            new ParsedSegment("other", false)
        );
    }

    @Test
    public void parsePathAsSegments__verify_that_an_unclosed_end_pattern_is_parsed_as_a_regular_segment() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test/unclosed-pattern}/other")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("unclosed-pattern}", false),
            new ParsedSegment("other", false)
        );
    }

    @Test
    public void parsePathAsSegments__verify_that_an_inside_pattern_is_parsed_as_a_regular_segment() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test/unclosed-{middle}-pattern/other")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("unclosed-{middle}-pattern", false),
            new ParsedSegment("other", false)
        );
    }

    @Test
    public void parsePathAsSegments__verify_that_segment_patterns_are_correctly_recognized() {
        Assertions.assertThat(HttpRoutes.parsePathAsSegments("/test/{arg1}/{arg2}/last-segment")).containsExactly(
            new ParsedSegment("test", false),
            new ParsedSegment("arg1", true),
            new ParsedSegment("arg2", true),
            new ParsedSegment("last-segment", false)
        );
    }

    @Test
    public void parsePath__verify_that_a_path_is_correctly_parsed() {
        String basePath = "/seg1/seg2/{arg}";
        ParsedPath parsedPath = HttpRoutes.parsePath(basePath);
        Assertions.assertThat(parsedPath).isNotNull();
        Assertions.assertThat(parsedPath.originalPath()).isEqualTo(basePath);
        Assertions.assertThat(parsedPath.genericPath()).isEqualTo("/seg1/seg2/{}");
        Assertions.assertThat(parsedPath.segments()).isEqualTo(List.of(
            new ParsedSegment("seg1", false),
            new ParsedSegment("seg2", false),
            new ParsedSegment("arg", true)
        ));
    }

    @Test
    public void parseRoute__verify_that_a_route_is_correctly_parsed() {
        ParsedRoute<String> parsedRoute = makeParsedRoute();
        Assertions.assertThat(parsedRoute).isNotNull();
        Assertions.assertThat(parsedRoute.parsedPath()).isNotNull();
        Assertions.assertThat(parsedRoute.httpMethod()).isEqualTo("GET");
        Assertions.assertThat(parsedRoute.attachedData()).isEqualTo("attached-data");
    }

    //      * - [] => "/"
    //     * - ["test"] => "/test"
    //     * - ["test", "arg" (isPattern), "other" => "/test/" + segmentPatternNameMaker("arg") + "/other"

    @Test
    public void serializeParsedPath__empty_segments_should_return_a_slash_path() {
        Assertions.assertThat(HttpRoutes.serializeParsedPath(List.of(), a -> a)).isEqualTo("/");
    }

    @Test
    public void serializeParsedPath__single_non_pattern_segment_should_return_a_path_with_slash_and_the_name_of_the_segment() {
        Assertions.assertThat(
            HttpRoutes.serializeParsedPath(List.of(new ParsedSegment("test", false)), a -> a))
            .isEqualTo("/test");
    }

    @Test
    public void serializeParsedPath__segments_with_pattern_and_non_pattern_segments_should_be_correctly_serialized() {
        Assertions.assertThat(
            HttpRoutes.serializeParsedPath(
                List.of(
                    new ParsedSegment("test", false),
                    new ParsedSegment("arg", true),
                    new ParsedSegment("other", false)
                ),
                a -> "-" + a
            ))
            .isEqualTo("/test/-arg/other");
    }

    @NotNull
    static ParsedRoute<String> makeParsedRoute() {
        return HttpRoutes.parseRoute("/test/{arg-name}", "GET", "attached-data");
    }
}
