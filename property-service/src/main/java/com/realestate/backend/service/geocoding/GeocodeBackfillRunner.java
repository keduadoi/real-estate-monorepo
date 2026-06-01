package com.realestate.backend.service.geocoding;

import com.realestate.backend.entity.GeocodingStatus;
import com.realestate.backend.entity.Property;
import com.realestate.backend.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-shot geocoding backfill for existing rows whose status is PENDING or FAILED.
 *
 * Runs only when the {@code backfill} profile is active:
 *   {@code java -jar property-service.jar --spring.profiles.active=backfill}
 *
 * The runner is idempotent — re-running it only re-attempts rows that are still
 * PENDING or FAILED. SUCCESS rows are left untouched.
 *
 * Throughput is bounded by the {@code nominatim} rate limiter (~1 req/sec by
 * policy), so ~1,000 properties take ~17 minutes.
 */
@Component
@Profile("backfill")
@RequiredArgsConstructor
@Slf4j
public class GeocodeBackfillRunner implements ApplicationRunner {

    private static final int PAGE_SIZE = 100;

    private final PropertyRepository propertyRepository;
    private final GeocodingService geocodingService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Geocode backfill starting ===");
        long startMs = System.currentTimeMillis();
        int success = 0, failed = 0, skipped = 0, errors = 0;

        int pageNum = 0;
        Page<Property> page;
        do {
            page = propertyRepository.findByGeocodingStatusIn(
                    List.of(GeocodingStatus.PENDING, GeocodingStatus.FAILED),
                    // Always page 0 — geocodeSync mutates status so the next query
                    // returns a fresh slice of still-unresolved rows.
                    PageRequest.of(0, PAGE_SIZE, Sort.by("id"))
            );
            log.info("Backfill batch {}: {} rows to process", pageNum, page.getNumberOfElements());

            for (Property property : page.getContent()) {
                try {
                    GeocodingStatus result = geocodingService.geocodeSync(property);
                    switch (result) {
                        case SUCCESS -> success++;
                        case FAILED  -> failed++;
                        case SKIPPED -> skipped++;
                        case PENDING -> {
                            // Transient failure — count as error and continue;
                            // the row stays PENDING for the next backfill run.
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("Backfill: unexpected error on property {}: {}",
                            property.getId(), e.getMessage(), e);
                }
            }
            pageNum++;
        } while (!page.isEmpty() && (success + failed + skipped + errors) < 100_000);
        // hard ceiling guards against pathological loops.

        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        log.info("=== Geocode backfill complete in {}s: success={} failed={} skipped={} errors={} ===",
                elapsed, success, failed, skipped, errors);
    }
}
