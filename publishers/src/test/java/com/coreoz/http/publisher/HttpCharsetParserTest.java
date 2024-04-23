package com.coreoz.http.publisher;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class HttpCharsetParserTest {
    @Test
    public void null_content_type_should_return_iso_charset() {
        Assertions.assertThat(HttpCharsetParser.parseEncodingFromHttpContentType(null))
            .isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void empty_content_type_should_return_iso_charset() {
        Assertions.assertThat(HttpCharsetParser.parseEncodingFromHttpContentType(""))
            .isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void content_type_without_charset_should_return_iso_charset() {
        Assertions.assertThat(HttpCharsetParser.parseEncodingFromHttpContentType("application/json"))
            .isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void unknown_charset_should_return_iso_charset() {
        Assertions.assertThat(HttpCharsetParser.parseEncodingFromHttpContentType("charset=azerty"))
            .isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void utf8_charset_should_return_utf8_charset() {
        Assertions
            .assertThat(HttpCharsetParser.parseEncodingFromHttpContentType("charset=utf8"))
            .isEqualTo(StandardCharsets.UTF_8);
        Assertions
            .assertThat(HttpCharsetParser.parseEncodingFromHttpContentType("application/json; charset=utf-8"))
            .isEqualTo(StandardCharsets.UTF_8);
        Assertions
            .assertThat(HttpCharsetParser.parseEncodingFromHttpContentType("charset=utf-8; application/json"))
            .isEqualTo(StandardCharsets.UTF_8);
    }
}
