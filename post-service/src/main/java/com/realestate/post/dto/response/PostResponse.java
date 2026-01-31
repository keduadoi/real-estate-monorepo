package com.realestate.post.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PostResponse(
    UUID id,
    String content,
    PostAuthorResponse author,
    long likeCount,
    boolean isLikedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
