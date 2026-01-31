package com.realestate.post.dto.response;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    long total,
    int page,
    int perPage,
    int totalPages
) {}
