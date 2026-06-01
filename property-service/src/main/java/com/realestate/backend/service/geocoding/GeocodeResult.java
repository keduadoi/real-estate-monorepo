package com.realestate.backend.service.geocoding;

/**
 * Vendor-neutral geocoding outcome.
 * Coordinates are WGS84 so they survive provider swaps (Nominatim → Google → Mapbox).
 *
 * @param latitude  WGS84 latitude
 * @param longitude WGS84 longitude
 * @param confidence 0.0–1.0 quality score reported by the provider, when available
 * @param provider  short identifier of the provider that produced this result (e.g. "nominatim")
 */
public record GeocodeResult(
        double latitude,
        double longitude,
        double confidence,
        String provider
) {
}
