package com.realestate.aisearch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Chip(
        String id,
        String label,
        String field,
        Object value,
        String confidence
) {
}
