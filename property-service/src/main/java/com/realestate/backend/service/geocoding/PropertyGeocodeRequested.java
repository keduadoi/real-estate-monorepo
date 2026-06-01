package com.realestate.backend.service.geocoding;

/**
 * Application event signalling that a property needs (re)geocoding.
 * Listened to by {@link GeocodingService} after the originating transaction
 * commits, so the listener is guaranteed to find the row.
 */
public record PropertyGeocodeRequested(Long propertyId) {
}
