package com.realestate.news.mapper;

import com.realestate.news.dto.NewsArticleResponse;
import com.realestate.news.dto.NewsArticleSummary;
import com.realestate.news.entity.NewsArticle;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    public NewsArticleResponse toResponse(NewsArticle entity) {
        return NewsArticleResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .content(entity.getContent())
                .author(entity.getAuthor())
                .category(entity.getCategory())
                .imageUrl(entity.getImageUrl())
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NewsArticleSummary toSummary(NewsArticle entity) {
        return NewsArticleSummary.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .summary(entity.getSummary())
                .author(entity.getAuthor())
                .category(entity.getCategory())
                .imageUrl(entity.getImageUrl())
                .publishedAt(entity.getPublishedAt())
                .build();
    }
}
