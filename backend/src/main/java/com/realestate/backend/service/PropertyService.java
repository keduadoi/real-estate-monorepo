package com.realestate.backend.service;

import com.realestate.backend.dto.*;
import com.realestate.backend.entity.Property;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.UnauthorizedException;
import com.realestate.backend.mapper.PropertyMapper;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.security.UserContext;
import com.realestate.backend.security.UserInfo;
import com.realestate.backend.specification.PropertySpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for Property business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final StorageService storageService;

    /**
     * Get all properties with pagination
     */
    public PageResponse<PropertyDTO> getAllProperties(int page, int size, String sortBy, String sortDirection) {
        log.debug("Getting all properties - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                  page, size, sortBy, sortDirection);

        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Property> propertyPage = propertyRepository.findAll(pageable);

        return buildPageResponse(propertyPage);
    }

    /**
     * Search and filter properties with pagination
     */
    public PageResponse<PropertyDTO> searchProperties(PropertySearchRequest searchRequest, int page, int size) {
        log.debug("Searching properties with filters: {}", searchRequest);

        String sortBy = searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "createdAt";
        String sortDirection = searchRequest.getSortDirection() != null ? searchRequest.getSortDirection() : "desc";

        Sort sort = createSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Property> spec = PropertySpecification.withFilters(searchRequest);
        Page<Property> propertyPage = propertyRepository.findAll(spec, pageable);

        return buildPageResponse(propertyPage);
    }

    /**
     * Get property by ID
     */
    public PropertyDTO getPropertyById(Long id) {
        log.debug("Getting property by ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with ID: " + id));

        return propertyMapper.toDTO(property);
    }

    /**
     * Get properties by user ID with pagination
     */
    public PageResponse<PropertyDTO> getPropertiesByUserId(String userId, int page, int size) {
        log.debug("Getting properties by user ID: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> propertyPage = propertyRepository.findByUserId(userId, pageable);

        return buildPageResponse(propertyPage);
    }

    /**
     * Get properties for the current authenticated user
     */
    public PageResponse<PropertyDTO> getCurrentUserProperties(int page, int size) {
        UserInfo user = requireAuthentication();
        log.debug("Getting properties for current user: {}", user.getId());

        return getPropertiesByUserId(user.getId(), page, size);
    }

    /**
     * Get distinct cities
     */
    public List<String> getDistinctCities() {
        log.debug("Getting distinct cities");
        return propertyRepository.findDistinctCities();
    }

    /**
     * Create a new property.
     * If userId is not provided in request, uses the current authenticated user.
     */
    @Transactional
    public PropertyDTO createProperty(CreatePropertyRequest request) {
        log.debug("Creating new property: {}", request.getTitle());

        // If userId not provided, use current authenticated user
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            UserInfo user = requireAuthentication();
            request.setUserId(user.getId());
            log.debug("Set userId from authenticated user: {}", user.getId());
        }

        Property property = propertyMapper.toEntity(request);
        Property savedProperty = propertyRepository.save(property);

        log.info("Property created successfully with ID: {} by user: {}",
                savedProperty.getId(), savedProperty.getUserId());
        return propertyMapper.toDTO(savedProperty);
    }

    /**
     * Update an existing property.
     * Validates ownership - only property owner or admin can update.
     */
    @Transactional
    public PropertyDTO updateProperty(Long id, UpdatePropertyRequest request) {
        log.debug("Updating property with ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with ID: " + id));

        // Validate ownership
        validateOwnership(property);

        propertyMapper.updateEntity(property, request);
        Property updatedProperty = propertyRepository.save(property);

        log.info("Property updated successfully with ID: {} by user: {}",
                id, UserContext.getCurrentUserId().orElse("unknown"));
        return propertyMapper.toDTO(updatedProperty);
    }

    /**
     * Delete a property by ID.
     * Validates ownership - only property owner or admin can delete.
     */
    @Transactional
    public void deleteProperty(Long id) {
        log.debug("Deleting property with ID: {}", id);

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with ID: " + id));

        // Validate ownership
        validateOwnership(property);

        // Delete associated images
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            log.debug("Deleting {} image(s) for property {}", property.getImages().size(), id);
            storageService.deleteFiles(property.getImages());
        }

        propertyRepository.deleteById(id);
        log.info("Property deleted successfully with ID: {} by user: {}",
                id, UserContext.getCurrentUserId().orElse("unknown"));
    }

    /**
     * Find property by ID (internal use)
     */
    public Property findById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with ID: " + id));
    }

    /**
     * Check if property exists
     */
    public boolean existsById(Long id) {
        return propertyRepository.existsById(id);
    }

    /**
     * Count properties by status
     */
    public long countByStatus(String status) {
        try {
            return propertyRepository.countByStatus(
                com.realestate.backend.entity.PropertyStatus.valueOf(status.toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: {}", status);
            return 0;
        }
    }

    // Helper methods

    /**
     * Create Sort object from parameters
     */
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Default to createdAt if invalid sort field
        String validSortBy = isValidSortField(sortBy) ? sortBy : "createdAt";

        return Sort.by(direction, validSortBy);
    }

    /**
     * Validate sort field
     */
    private boolean isValidSortField(String field) {
        if (field == null) return false;
        return List.of("id", "title", "price", "area", "bedrooms", "bathrooms",
                      "city", "createdAt", "updatedAt").contains(field);
    }

    /**
     * Build paginated response from Page object
     */
    private PageResponse<PropertyDTO> buildPageResponse(Page<Property> propertyPage) {
        List<PropertyDTO> propertyDTOs = propertyPage.getContent().stream()
                .map(propertyMapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<PropertyDTO>builder()
                .data(propertyDTOs)
                .total(propertyPage.getTotalElements())
                .page(propertyPage.getNumber())
                .perPage(propertyPage.getSize())
                .totalPages(propertyPage.getTotalPages())
                .build();
    }

    // ========================================
    // Security Helper Methods
    // ========================================

    /**
     * Require authentication - throws UnauthorizedException if not authenticated
     */
    private UserInfo requireAuthentication() {
        return UserContext.getCurrentUser()
                .filter(UserInfo::isAuthenticated)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    /**
     * Validate that the current user owns the property or is an admin.
     * Throws ForbiddenException if validation fails.
     */
    private void validateOwnership(Property property) {
        UserInfo user = requireAuthentication();

        // Admins can modify any property
        if (user.isAdmin()) {
            log.debug("Admin user {} accessing property {}", user.getId(), property.getId());
            return;
        }

        // Check if current user owns the property
        if (!user.getId().equals(property.getUserId())) {
            log.warn("User {} attempted to access property {} owned by {}",
                    user.getId(), property.getId(), property.getUserId());
            throw new ForbiddenException("You do not have permission to modify this property");
        }
    }

    /**
     * Custom exception for property not found
     */
    public static class PropertyNotFoundException extends RuntimeException {
        public PropertyNotFoundException(String message) {
            super(message);
        }
    }
}
