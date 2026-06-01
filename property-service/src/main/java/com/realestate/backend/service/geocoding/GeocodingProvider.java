package com.realestate.backend.service.geocoding;

import java.util.Optional;

/**
 * Strategy interface for address → coordinate resolution.
 *
 * Implementations are selected by the {@code app.maps.geocoding-provider} config
 * property. Swap providers (Nominatim → Google → Mapbox) by changing that one
 * value; callers do not depend on the concrete class.
 */
public interface GeocodingProvider {

    /**
     * Resolve an address to coordinates.
     *
     * @return present if the provider returned a usable result; empty if the
     *         address could not be resolved (no match, or non-retryable error).
     *         Transient failures (network/HTTP 5xx) should be surfaced as
     *         {@link GeocodingException} so the orchestrator can retry/redrive.
     */
    Optional<GeocodeResult> geocode(String address, String city);

    /** Short identifier used in {@link GeocodeResult#provider()} and config. */
    String name();
}
