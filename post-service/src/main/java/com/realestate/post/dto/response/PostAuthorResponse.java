package com.realestate.post.dto.response;

import lombok.Builder;

@Builder
public record PostAuthorResponse(
    String id,
    String name,
    String email
) {}
