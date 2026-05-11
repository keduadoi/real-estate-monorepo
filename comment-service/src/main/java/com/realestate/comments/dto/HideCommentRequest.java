package com.realestate.comments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideCommentRequest(
        @NotBlank
        @Size(max = 200) String reason
) {
}
