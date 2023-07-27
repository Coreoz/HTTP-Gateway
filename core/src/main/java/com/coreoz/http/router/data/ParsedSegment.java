package com.coreoz.http.router.data;

import lombok.*;

@Value
public class ParsedSegment {
    String name;
    boolean isPattern;
}
