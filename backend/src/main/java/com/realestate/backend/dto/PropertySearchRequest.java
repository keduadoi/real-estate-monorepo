package com.realestate.backend.dto;

import com.realestate.backend.entity.PropertyStatus;
import com.realestate.backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for searching and filtering properties.
 * Matches the UI SearchFilters interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertySearchRequest {

    /**
     * Search query for title, description, or address
     */
    private String query;

    /**
     * Filter by city
     */
    private String city;

    /**
     * Filter by property type
     */
    private PropertyType propertyType;

    /**
     * Filter by status (for-sale or for-rent)
     */
    private PropertyStatus status;

    /**
     * Minimum price filter
     */
    private BigDecimal minPrice;

    /**
     * Maximum price filter
     */
    private BigDecimal maxPrice;

    /**
     * Filter by number of bedrooms (exact match or minimum)
     */
    private Integer bedrooms;

    /**
     * Filter by user ID
     */
    private String userId;

    /**
     * Sort field (e.g., "price", "createdAt", "area")
     */
    private String sortBy;

    /**
     * Sort direction ("asc" or "desc")
     */
    private String sortDirection;
}
