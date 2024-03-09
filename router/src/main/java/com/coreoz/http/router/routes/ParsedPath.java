package com.coreoz.http.router.routes;

import com.coreoz.http.router.data.ParsedSegment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a path (e.g. <code>/path/{path-arg}/other-path-segment</code>) that has been parsed.
 * @param segments The list of path parts that has been parsed. E.g. <code>({name='path', isPattern=false}, {name='path-arg', isPattern=true}, {name=other-path-segment, isPattern=false})</code>
 * @param genericPath The path where path pattern names have been removed. E.g. <code>/path/{}/other-path-segment</code>
 * @param originalPath The base path. E.g. <code>/path/{path-arg}/other-path-segment</code>
 */
public record ParsedPath(@NotNull List<ParsedSegment> segments, @NotNull String genericPath,
                         @NotNull String originalPath) {
}
