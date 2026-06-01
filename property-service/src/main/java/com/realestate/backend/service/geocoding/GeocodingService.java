package com.realestate.backend.service.geocoding;

import com.realestate.backend.entity.GeocodingStatus;
import com.realestate.backend.entity.Property;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Orchestrates geocoding for a single property: calls the active provider,
 * writes the outcome back to the property row, sets the status field.
 *
 * Entry points:
 *   • {@link #onPropertyGeocodeRequested(PropertyGeocodeRequested)} — async listener
 *     fired after PropertyService commits a create/update; runs on the geocoding executor.
 *   • {@link #geocodeSync(Property)} — blocking, used by the backfill runner
 *     and the admin re-trigger endpoint (where the caller wants to know the result).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final GeocodingProvider provider;
    private final PropertyRepository propertyRepository;

    /**
     * Fires after the originating transaction commits, so the property row is
     * guaranteed visible. Runs on the geocoding executor to keep request threads free.
     * Failures are logged but never propagated.
     */
    @Async("geocodingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPropertyGeocodeRequested(PropertyGeocodeRequested event) {
        Long propertyId = event.propertyId();
        propertyRepository.findById(propertyId).ifPresentOrElse(
                this::geocodeSync,
                () -> log.warn("geocode listener: property {} not found", propertyId)
        );
    }

    /**
     * Geocode the given property and persist the outcome.
     * Safe to call inside an existing transaction.
     *
     * @return the final status written to the row
     */
    @Transactional
    public GeocodingStatus geocodeSync(Property property) {
        Long id = property.getId();
        String address = property.getAddress();
        String city = property.getCity();

        if (isBlank(address) && isBlank(city)) {
            log.debug("Skipping geocode for property {}: no address/city", id);
            return persistOutcome(property, GeocodingStatus.SKIPPED, null);
        }

        try {
            Optional<GeocodeResult> result = provider.geocode(address, city);
            if (result.isEmpty()) {
                log.info("Geocoding FAILED (no match) for property {} via {}", id, provider.name());
                return persistOutcome(property, GeocodingStatus.FAILED, null);
            }
            log.info("Geocoding SUCCESS for property {} via {} → ({}, {})",
                    id, provider.name(), result.get().latitude(), result.get().longitude());
            return persistOutcome(property, GeocodingStatus.SUCCESS, result.get());

        } catch (GeocodingException e) {
            // Transient: keep PENDING so the backfill runner picks it up.
            log.warn("Geocoding transient failure for property {}: {} — leaving status as PENDING",
                    id, e.getMessage());
            property.setGeocodingStatus(GeocodingStatus.PENDING);
            propertyRepository.save(property);
            return GeocodingStatus.PENDING;
        }
    }

    private GeocodingStatus persistOutcome(Property property, GeocodingStatus status, GeocodeResult result) {
        property.setGeocodingStatus(status);
        property.setGeocodedAt(LocalDateTime.now());
        if (result != null) {
            property.setLatitude(result.latitude());
            property.setLongitude(result.longitude());
        } else if (status == GeocodingStatus.FAILED || status == GeocodingStatus.SKIPPED) {
            property.setLatitude(null);
            property.setLongitude(null);
        }
        propertyRepository.save(property);
        return status;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
