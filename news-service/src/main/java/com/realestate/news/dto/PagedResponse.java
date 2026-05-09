package com.realestate.news.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PagedResponse<T>(
        List<T> data,
        long total,
        int page,
        int perPage,
        int totalPages
) {}
