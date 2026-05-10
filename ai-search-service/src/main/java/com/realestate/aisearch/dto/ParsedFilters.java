package com.realestate.aisearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParsedFilters(
        String propertyType,
        String status,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minArea,
        BigDecimal maxArea,
        List<String> cities,
        List<String> districts,
        String freeText
) {
}
