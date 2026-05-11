package com.realestate.comments.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @Size(max = 1000) String body
) {
}
