package com.realestate.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateNewsArticleRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank String content,
        @Size(max = 100) String author,
        @Size(max = 80) String category,
        @Size(max = 1000) String imageUrl,
        LocalDateTime publishedAt
) {}
