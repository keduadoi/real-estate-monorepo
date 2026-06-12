package com.realestate.post.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReplyResponse(
    UUID id,
    UUID postId,
    String content,
    PostAuthorResponse author,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
