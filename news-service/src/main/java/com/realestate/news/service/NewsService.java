package com.realestate.news.service;

import com.realestate.news.dto.CreateNewsArticleRequest;
import com.realestate.news.dto.NewsArticleResponse;
import com.realestate.news.dto.NewsArticleSummary;
import com.realestate.news.dto.PagedResponse;
import com.realestate.news.dto.UpdateNewsArticleRequest;
import com.realestate.news.entity.NewsArticle;
import com.realestate.news.exception.NewsArticleNotFoundException;
import com.realestate.news.mapper.NewsMapper;
import com.realestate.news.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsArticleRepository repository;
    private final NewsMapper mapper;

    @Transactional(readOnly = true)
    public PagedResponse<NewsArticleSummary> list(int page, int perPage, String category) {
        Pageable pageable = PageRequest.of(page, perPage,
                Sort.by(Sort.Direction.DESC, "publishedAt", "id"));

        Page<NewsArticle> result = (category == null || category.isBlank())
                ? repository.findAll(pageable)
                : repository.findByCategoryIgnoreCase(category, pageable);

        return PagedResponse.<NewsArticleSummary>builder()
                .data(result.map(mapper::toSummary).getContent())
                .total(result.getTotalElements())
                .page(result.getNumber())
                .perPage(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public NewsArticleResponse getById(Long id) {
        NewsArticle article = repository.findById(id)
                .orElseThrow(() -> new NewsArticleNotFoundException(id));
        return mapper.toResponse(article);
    }

    public NewsArticleResponse create(CreateNewsArticleRequest request) {
        NewsArticle article = NewsArticle.builder()
                .title(request.title())
                .summary(request.summary())
                .content(request.content())
                .author(request.author())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .publishedAt(request.publishedAt() != null ? request.publishedAt() : LocalDateTime.now())
                .build();
        NewsArticle saved = repository.save(article);
        log.info("Created news article id={} title={}", saved.getId(), saved.getTitle());
        return mapper.toResponse(saved);
    }

    public NewsArticleResponse update(Long id, UpdateNewsArticleRequest request) {
        NewsArticle article = repository.findById(id)
                .orElseThrow(() -> new NewsArticleNotFoundException(id));

        if (request.title() != null) article.setTitle(request.title());
        if (request.summary() != null) article.setSummary(request.summary());
        if (request.content() != null) article.setContent(request.content());
        if (request.author() != null) article.setAuthor(request.author());
        if (request.category() != null) article.setCategory(request.category());
        if (request.imageUrl() != null) article.setImageUrl(request.imageUrl());
        if (request.publishedAt() != null) article.setPublishedAt(request.publishedAt());

        NewsArticle saved = repository.save(article);
        log.info("Updated news article id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NewsArticleNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Deleted news article id={}", id);
    }
}
