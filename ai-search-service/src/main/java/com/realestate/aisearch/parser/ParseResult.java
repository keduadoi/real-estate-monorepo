package com.realestate.aisearch.parser;

import com.realestate.aisearch.dto.Chip;
import com.realestate.aisearch.dto.ParsedFilters;

import java.util.List;

public record ParseResult(
        ParsedFilters filters,
        List<Chip> chips,
        List<String> warnings
) {
}
