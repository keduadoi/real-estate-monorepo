package com.realestate.news.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NewsArticleResponse(
        Long id,
        String title,
        String summary,
        String content,
        String author,
        String category,
        String imageUrl,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
