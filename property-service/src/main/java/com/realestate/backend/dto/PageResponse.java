package com.realestate.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Matches the UI PaginatedResult interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * List of items in current page
     */
    private List<T> data;

    /**
     * Total number of items across all pages
     */
    private long total;

    /**
     * Current page number (0-indexed)
     */
    private int page;

    /**
     * Number of items per page
     */
    private int perPage;

    /**
     * Total number of pages
     */
    private int totalPages;
}
