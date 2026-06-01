package com.realestate.backend.repository;

import com.realestate.backend.entity.GeocodingStatus;
import com.realestate.backend.entity.Property;
import com.realestate.backend.entity.PropertyStatus;
import com.realestate.backend.entity.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository interface for Property entity.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    /**
     * Find all properties by city
     */
    Page<Property> findByCity(String city, Pageable pageable);

    /**
     * Find all properties by property type
     */
    Page<Property> findByPropertyType(PropertyType propertyType, Pageable pageable);

    /**
     * Find all properties by status
     */
    Page<Property> findByStatus(PropertyStatus status, Pageable pageable);

    /**
     * Find all properties by city and status
     */
    Page<Property> findByCityAndStatus(String city, PropertyStatus status, Pageable pageable);

    /**
     * Find all properties by user ID
     */
    Page<Property> findByUserId(String userId, Pageable pageable);

    /**
     * Find properties with price in range
     */
    Page<Property> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Find properties by number of bedrooms
     */
    Page<Property> findByBedrooms(Integer bedrooms, Pageable pageable);

    /**
     * Find properties by minimum number of bedrooms
     */
    Page<Property> findByBedroomsGreaterThanEqual(Integer bedrooms, Pageable pageable);

    /**
     * Search properties by title or description (case-insensitive)
     */
    @Query("SELECT p FROM Property p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.address) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Property> searchProperties(@Param("query") String query, Pageable pageable);

    /**
     * Get distinct cities with properties
     */
    @Query("SELECT DISTINCT p.city FROM Property p ORDER BY p.city")
    List<String> findDistinctCities();

    /**
     * Count properties by status
     */
    long countByStatus(PropertyStatus status);

    /**
     * Count properties by user ID
     */
    long countByUserId(String userId);

    /**
     * Stream properties whose geocoding status is in the given set, paged.
     * Used by the backfill runner to find PENDING/FAILED rows.
     */
    Page<Property> findByGeocodingStatusIn(Collection<GeocodingStatus> statuses, Pageable pageable);
}
