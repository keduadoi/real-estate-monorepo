package com.realestate.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for database management operations.
 * WARNING: This should be protected with authentication in production.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Clear all data from the database.
     * WARNING: This deletes ALL properties, images, and features.
     */
    @DeleteMapping("/clear-database")
    @Transactional
    public ResponseEntity<Map<String, Object>> clearDatabase() {
        log.warn("Clearing all data from database...");

        try {
            // Delete in order due to foreign key constraints
            int imagesDeleted = jdbcTemplate.update("DELETE FROM property_images");
            int featuresDeleted = jdbcTemplate.update("DELETE FROM property_features");
            int propertiesDeleted = jdbcTemplate.update("DELETE FROM properties");

            log.info("Database cleared: {} properties, {} images, {} features deleted",
                    propertiesDeleted, imagesDeleted, featuresDeleted);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database cleared successfully");
            response.put("deleted", Map.of(
                    "properties", propertiesDeleted,
                    "images", imagesDeleted,
                    "features", featuresDeleted
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to clear database", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to clear database: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get database statistics.
     */
    @RequestMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        try {
            Integer propertyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM properties", Integer.class);
            Integer imageCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_images", Integer.class);
            Integer featureCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_features", Integer.class);

            Map<String, Object> stats = new HashMap<>();
            stats.put("properties", propertyCount != null ? propertyCount : 0);
            stats.put("images", imageCount != null ? imageCount : 0);
            stats.put("features", featureCount != null ? featureCount : 0);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Failed to get database stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
