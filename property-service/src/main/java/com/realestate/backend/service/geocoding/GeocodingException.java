package com.realestate.backend.service.geocoding;

/**
 * Thrown when a geocoding provider fails transiently (network error, HTTP 5xx,
 * rate-limit exhaustion). The orchestrator should keep the property's status as
 * PENDING and let the backfill runner retry later.
 *
 * Non-retryable outcomes (no result found, malformed address) are signalled
 * via {@code Optional.empty()} instead.
 */
public class GeocodingException extends RuntimeException {
    public GeocodingException(String message) {
        super(message);
    }

    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
