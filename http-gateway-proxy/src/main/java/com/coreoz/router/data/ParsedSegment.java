package com.coreoz.router.data;

import lombok.*;

@Value
public class ParsedSegment {
    String name;
    boolean isPattern;
}
