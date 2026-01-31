package com.realestate.auth.controller;

import com.realestate.auth.service.KeyRotationScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin endpoints for auth service management.
 * These endpoints should be protected by ACL in Kong (admin role only).
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final KeyRotationScheduler keyRotationScheduler;

    public AdminController(KeyRotationScheduler keyRotationScheduler) {
        this.keyRotationScheduler = keyRotationScheduler;
    }

    /**
     * Get JWT key statistics.
     * Returns information about active keys, key IDs, and expiration dates.
     */
    @GetMapping("/keys/stats")
    public ResponseEntity<Map<String, Object>> getKeyStats() {
        Map<String, Object> stats = keyRotationScheduler.getKeyStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Manually trigger key rotation.
     * Creates a new key pair and sets up grace period for old keys.
     * Should be used sparingly - automatic rotation is preferred.
     */
    @PostMapping("/keys/rotate")
    public ResponseEntity<Map<String, Object>> rotateKeys() {
        log.info("Manual key rotation requested via admin API");

        try {
            String newKeyId = keyRotationScheduler.triggerManualRotation();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("newKeyId", newKeyId);
            response.put("message", "Key rotation completed successfully");
            response.put("stats", keyRotationScheduler.getKeyStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Key rotation failed", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check endpoint for admin API.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service-admin");
        return ResponseEntity.ok(response);
    }
}
