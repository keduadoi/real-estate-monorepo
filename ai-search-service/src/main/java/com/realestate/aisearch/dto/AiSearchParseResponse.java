package com.realestate.aisearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiSearchParseResponse(
        String originalQuery,
        ParsedFilters filters,
        List<Chip> chips,
        List<String> warnings,
        String parserMode,
        long parserLatencyMs,
        boolean cacheHit
) {
}
