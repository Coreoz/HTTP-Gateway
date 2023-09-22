package com.coreoz.http.upstream.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Guess the Charset to use depending on the Content Type header of an HTTP request
 */
public class HttpCharsetParser {
    private static final Logger logger = LoggerFactory.getLogger(HttpCharsetParser.class);

    private static final String CONTENT_TYPE_CHARSET = "charset=";

    public static Charset parseEncodingFromHttpContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.ISO_8859_1;
        }
        int startCharsetIndex = contentType.indexOf(CONTENT_TYPE_CHARSET);
        if (startCharsetIndex < 0) {
            return StandardCharsets.ISO_8859_1;
        }
        try {
            return Charset.forName(parseCharset(contentType, startCharsetIndex));
        } catch (Exception e) {
            logger.warn("Could not parse charset from content-type {}", contentType, e);
            return StandardCharsets.ISO_8859_1;
        }
    }

    private static String parseCharset(String contentType, int startCharsetIndex) {
        int charsetIndex = startCharsetIndex + CONTENT_TYPE_CHARSET.length();
        int endCharsetIndex = contentType.indexOf(charsetIndex, ';');

        if (endCharsetIndex < 0) {
            return contentType.substring(charsetIndex);
        }
        return contentType.substring(charsetIndex, endCharsetIndex);
    }
}
