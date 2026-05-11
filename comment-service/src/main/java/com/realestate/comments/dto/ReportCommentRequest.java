package com.realestate.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReportCommentRequest(
        @NotBlank
        @Pattern(regexp = "spam|abuse|off-topic|other")
        String reason,
        @Size(max = 500) String note
) {
}
