package com.realestate.auth.service;

import com.realestate.auth.config.MetricsConfig.AuthMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled service for automatic JWT key rotation.
 * Runs daily to check if key rotation is needed and performs rotation if necessary.
 */
@Service
public class KeyRotationScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeyRotationScheduler.class);

    private final JwtService jwtService;
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeKeyCount = new AtomicInteger(0);

    @Value("${jwt.key-rotation.enabled:true}")
    private boolean rotationEnabled;

    @Value("${jwt.key-rotation.max-key-age-days:30}")
    private int maxKeyAgeDays;

    @Value("${jwt.key-rotation.grace-period-days:7}")
    private int gracePeriodDays;

    public KeyRotationScheduler(JwtService jwtService, MeterRegistry meterRegistry) {
        this.jwtService = jwtService;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        // Register gauge for active key count
        Gauge.builder("auth_jwt_active_keys", activeKeyCount, AtomicInteger::get)
                .description("Number of active JWT signing keys")
                .register(meterRegistry);

        // Update initial count
        updateKeyMetrics();
    }

    /**
     * Scheduled task that runs daily at 2 AM to check and perform key rotation.
     * The cron expression: "0 0 2 * * ?" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 2 (2 AM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: ? (any)
     */
    @Scheduled(cron = "${jwt.key-rotation.cron:0 0 2 * * ?}")
    public void scheduledKeyRotation() {
        if (!rotationEnabled) {
            log.debug("Key rotation is disabled");
            return;
        }

        log.info("Starting scheduled key rotation check...");

        try {
            if (jwtService.isRotationNeeded(maxKeyAgeDays)) {
                log.info("Key rotation needed. Max key age: {} days", maxKeyAgeDays);
                String newKeyId = jwtService.rotateKeys(gracePeriodDays);
                log.info("Key rotation completed successfully. New key ID: {}", newKeyId);

                // Record rotation event
                meterRegistry.counter("auth_jwt_key_rotations_total").increment();
            } else {
                log.info("Key rotation not needed. Keys are within age limit.");
            }

            updateKeyMetrics();

        } catch (Exception e) {
            log.error("Key rotation failed", e);
            meterRegistry.counter("auth_jwt_key_rotation_failures_total").increment();
        }
    }

    /**
     * Manual trigger for key rotation (can be called from admin API).
     *
     * @return The ID of the new key
     */
    public String triggerManualRotation() {
        log.info("Manual key rotation triggered");
        String newKeyId = jwtService.rotateKeys(gracePeriodDays);
        updateKeyMetrics();
        meterRegistry.counter("auth_jwt_key_rotations_total").increment();
        return newKeyId;
    }

    /**
     * Get current key statistics.
     */
    public Map<String, Object> getKeyStats() {
        return jwtService.getKeyStatistics();
    }

    private void updateKeyMetrics() {
        Map<String, Object> stats = jwtService.getKeyStatistics();
        activeKeyCount.set((Integer) stats.getOrDefault("activeKeyCount", 0));
    }
}
