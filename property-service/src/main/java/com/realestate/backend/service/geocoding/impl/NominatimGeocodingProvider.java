package com.realestate.backend.service.geocoding.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.realestate.backend.service.geocoding.GeocodeResult;
import com.realestate.backend.service.geocoding.GeocodingException;
import com.realestate.backend.service.geocoding.GeocodingProvider;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Free OpenStreetMap-backed geocoder.
 *
 * Policy compliance:
 *   • &le; 1 request/second (enforced by Resilience4j {@code nominatim} rate limiter)
 *   • User-Agent header identifying the app (required by Nominatim's policy)
 *   • Results are persisted in our DB so we never re-query for the same address
 */
@Component
@ConditionalOnProperty(name = "app.maps.geocoding-provider", havingValue = "nominatim", matchIfMissing = true)
@Slf4j
public class NominatimGeocodingProvider implements GeocodingProvider {

    private static final String NAME = "nominatim";

    private final RestClient restClient;
    private final String baseUrl;

    public NominatimGeocodingProvider(
            @Value("${app.maps.nominatim.base-url:https://nominatim.openstreetmap.org}") String baseUrl,
            @Value("${app.maps.nominatim.user-agent:RealEstateApp/1.0}") String userAgent,
            @Value("${app.maps.nominatim.accept-language:vi,en}") String acceptLanguage) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Override
    @RateLimiter(name = "nominatim")
    public Optional<GeocodeResult> geocode(String address, String city) {
        String query = buildQuery(address, city);
        if (query.isBlank()) {
            log.debug("Skipping geocode: empty query");
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .queryParam("addressdetails", 0)
                .build()
                .encode()
                .toUri();

        try {
            JsonNode results = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (results == null || !results.isArray() || results.isEmpty()) {
                log.info("Nominatim returned no match for query: {}", query);
                return Optional.empty();
            }

            JsonNode first = results.get(0);
            double lat = first.path("lat").asDouble(Double.NaN);
            double lon = first.path("lon").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                log.warn("Nominatim result missing lat/lon for query: {}", query);
                return Optional.empty();
            }

            double importance = first.path("importance").asDouble(0.0);
            return Optional.of(new GeocodeResult(lat, lon, importance, NAME));

        } catch (HttpClientErrorException e) {
            // 4xx — likely a bad query; treat as no result, do not retry.
            log.warn("Nominatim client error {} for query {}: {}", e.getStatusCode(), query, e.getMessage());
            return Optional.empty();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            // 5xx or network — retryable.
            throw new GeocodingException("Nominatim transient failure: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new GeocodingException("Nominatim unexpected failure: " + e.getMessage(), e);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    private String buildQuery(String address, String city) {
        String a = address == null ? "" : address.trim();
        String c = city == null ? "" : city.trim();
        if (a.isEmpty() && c.isEmpty()) return "";
        if (a.isEmpty()) return c;
        if (c.isEmpty()) return a;
        return a + ", " + c;
    }
}
