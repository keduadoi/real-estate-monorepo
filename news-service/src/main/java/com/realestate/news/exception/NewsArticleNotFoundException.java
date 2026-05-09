package com.realestate.news.exception;

public class NewsArticleNotFoundException extends RuntimeException {
    public NewsArticleNotFoundException(Long id) {
        super("News article not found: id=" + id);
    }
}
