package com.realestate.backend.specification;

import com.realestate.backend.dto.PropertySearchRequest;
import com.realestate.backend.entity.Property;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for dynamic Property queries.
 * Builds query predicates based on search criteria.
 */
public class PropertySpecification {

    /**
     * Create a specification from search request
     */
    public static Specification<Property> withFilters(PropertySearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search in title, description, or address
            if (searchRequest.getQuery() != null && !searchRequest.getQuery().trim().isEmpty()) {
                String searchPattern = "%" + searchRequest.getQuery().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), searchPattern);
                Predicate addressMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("address")), searchPattern);
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch, addressMatch));
            }

            // Filter by city
            if (searchRequest.getCity() != null && !searchRequest.getCity().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("city"), searchRequest.getCity()));
            }

            // Filter by property type
            if (searchRequest.getPropertyType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("propertyType"), searchRequest.getPropertyType()));
            }

            // Filter by status
            if (searchRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), searchRequest.getStatus()));
            }

            // Filter by price range
            if (searchRequest.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), searchRequest.getMinPrice()));
            }
            if (searchRequest.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), searchRequest.getMaxPrice()));
            }

            // Filter by bedrooms (minimum)
            if (searchRequest.getBedrooms() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("bedrooms"), searchRequest.getBedrooms()));
            }

            // Filter by user ID
            if (searchRequest.getUserId() != null && !searchRequest.getUserId().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), searchRequest.getUserId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
