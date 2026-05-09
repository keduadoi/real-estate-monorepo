package com.realestate.news.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NewsArticleSummary(
        Long id,
        String title,
        String summary,
        String author,
        String category,
        String imageUrl,
        LocalDateTime publishedAt
) {}
