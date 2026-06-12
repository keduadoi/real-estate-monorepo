package com.realestate.post.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record PostResponse(
    UUID id,
    String content,
    List<String> imageUrls,
    PostAuthorResponse author,
    long likeCount,
    boolean isLikedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
