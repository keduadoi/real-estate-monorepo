package com.realestate.aisearch.parser;

public interface QueryParser {

    /** Identifier returned in the response so the frontend can show a badge / route metrics. */
    ParserMode mode();

    /**
     * Parse a free-form natural-language query into structured filters + UI chips.
     *
     * @param query  user-supplied query, validated non-blank upstream
     * @param locale BCP-47 tag (e.g. "vi-VN", "en"). Hint only — parsers may ignore.
     * @return parser-built filters, chips, and warnings
     */
    ParseResult parse(String query, String locale);
}
