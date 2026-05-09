package com.realestate.news.controller;

import com.realestate.news.dto.CreateNewsArticleRequest;
import com.realestate.news.dto.NewsArticleResponse;
import com.realestate.news.dto.NewsArticleSummary;
import com.realestate.news.dto.PagedResponse;
import com.realestate.news.dto.UpdateNewsArticleRequest;
import com.realestate.news.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<PagedResponse<NewsArticleSummary>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(newsService.list(page, perPage, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsArticleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getById(id));
    }

    @PostMapping
    public ResponseEntity<NewsArticleResponse> create(@Valid @RequestBody CreateNewsArticleRequest request) {
        NewsArticleResponse created = newsService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsArticleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNewsArticleRequest request) {
        return ResponseEntity.ok(newsService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
