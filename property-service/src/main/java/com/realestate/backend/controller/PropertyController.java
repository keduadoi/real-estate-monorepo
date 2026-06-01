package com.realestate.backend.controller;

import com.realestate.backend.dto.*;
import com.realestate.backend.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Property CRUD operations.
 * Base path: /api/properties
 *
 * CORS is handled by Kong Gateway - no @CrossOrigin needed.
 */
@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Slf4j
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * Get all properties with pagination
     * GET /api/properties?page=0&size=20&sortBy=createdAt&sortDirection=desc
     */
    @GetMapping
    public ResponseEntity<PageResponse<PropertyDTO>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("GET /api/properties - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                 page, size, sortBy, sortDirection);

        PageResponse<PropertyDTO> response = propertyService.getAllProperties(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    /**
     * Search and filter properties
     * POST /api/properties/search?page=0&size=20
     */
    @PostMapping("/search")
    public ResponseEntity<PageResponse<PropertyDTO>> searchProperties(
            @RequestBody PropertySearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("POST /api/properties/search - filters: {}, page: {}, size: {}",
                 searchRequest, page, size);

        PageResponse<PropertyDTO> response = propertyService.searchProperties(searchRequest, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get property by ID
     * GET /api/properties/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyDTO> getPropertyById(@PathVariable Long id) {
        log.info("GET /api/properties/{}", id);

        PropertyDTO property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(property);
    }

    /**
     * Get properties for the current authenticated user.
     * Uses user ID from Kong JWT headers (X-User-Id).
     * GET /api/properties/user?page=0&size=20
     */
    @GetMapping("/user")
    public ResponseEntity<PageResponse<PropertyDTO>> getCurrentUserProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/properties/user (current user) - page: {}, size: {}", page, size);

        PageResponse<PropertyDTO> response = propertyService.getCurrentUserProperties(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get properties by user ID (for admin or public profile viewing)
     * GET /api/properties/user/{userId}?page=0&size=20
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<PropertyDTO>> getPropertiesByUserId(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/properties/user/{} - page: {}, size: {}", userId, page, size);

        PageResponse<PropertyDTO> response = propertyService.getPropertiesByUserId(userId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get distinct cities
     * GET /api/properties/cities
     */
    @GetMapping("/cities")
    public ResponseEntity<List<String>> getDistinctCities() {
        log.info("GET /api/properties/cities");

        List<String> cities = propertyService.getDistinctCities();
        return ResponseEntity.ok(cities);
    }

    /**
     * Create a new property
     * POST /api/properties
     */
    @PostMapping
    public ResponseEntity<PropertyDTO> createProperty(@Valid @RequestBody CreatePropertyRequest request) {
        log.info("POST /api/properties - title: {}", request.getTitle());

        PropertyDTO createdProperty = propertyService.createProperty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProperty);
    }

    /**
     * Update an existing property
     * PUT /api/properties/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PropertyDTO> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request) {

        log.info("PUT /api/properties/{}", id);

        PropertyDTO updatedProperty = propertyService.updateProperty(id, request);
        return ResponseEntity.ok(updatedProperty);
    }

    /**
     * Delete a property
     * DELETE /api/properties/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        log.info("DELETE /api/properties/{}", id);

        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Re-trigger geocoding for one property. Owner or admin only.
     * POST /api/properties/{id}/geocode
     */
    @PostMapping("/{id}/geocode")
    public ResponseEntity<PropertyDTO> regeocodeProperty(@PathVariable Long id) {
        log.info("POST /api/properties/{}/geocode", id);

        PropertyDTO property = propertyService.regeocodeProperty(id);
        return ResponseEntity.ok(property);
    }

    /**
     * Check if property exists
     * HEAD /api/properties/{id}
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkPropertyExists(@PathVariable Long id) {
        log.info("HEAD /api/properties/{}", id);

        boolean exists = propertyService.existsById(id);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Count properties by status
     * GET /api/properties/count/{status}
     */
    @GetMapping("/count/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable String status) {
        log.info("GET /api/properties/count/{}", status);

        long count = propertyService.countByStatus(status);
        return ResponseEntity.ok(count);
    }
}
