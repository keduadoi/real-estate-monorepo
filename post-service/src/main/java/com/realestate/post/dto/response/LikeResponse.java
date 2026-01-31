package com.realestate.post.dto.response;

public record LikeResponse(
    boolean liked,
    long likeCount
) {}
