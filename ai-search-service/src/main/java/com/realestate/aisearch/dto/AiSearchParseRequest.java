package com.realestate.aisearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiSearchParseRequest(
        @NotBlank
        @Size(max = 500)
        String query,

        String locale
) {
}
