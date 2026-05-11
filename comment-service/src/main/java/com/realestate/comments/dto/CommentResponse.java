package com.realestate.comments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponse(
        Long id,
        Long propertyId,
        Long parentId,
        String userId,
        String displayName,
        String gravatarHash,
        boolean isOwnerOfProperty,
        boolean isAdmin,
        String body,
        Integer likeCount,
        boolean likedByCurrent,
        Integer replyCount,
        List<CommentResponse> replies,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean editable,
        boolean hidden
) {
}
