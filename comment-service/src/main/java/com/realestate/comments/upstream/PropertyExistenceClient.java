package com.realestate.comments.upstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.realestate.comments.config.CommentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Calls property-service to confirm a propertyId exists and to learn the owner's
 * userId (so we can render the "chủ tin" badge on owner-authored comments).
 *
 * Result cached for `comments.property-service.cache-ttl-seconds` via Caffeine.
 */
@Component
@Slf4j
public class PropertyExistenceClient {

    private final CommentProperties props;
    private final WebClient client;

    @Autowired
    public PropertyExistenceClient(CommentProperties props, WebClient.Builder builder) {
        this.props = props;
        this.client = builder
                .baseUrl(props.getPropertyService().getBaseUrl())
                .build();
    }

    public record PropertyMeta(boolean exists, String ownerUserId) {
        public static final PropertyMeta MISSING = new PropertyMeta(false, null);
    }

    /**
     * Cache key is just the id. {@link CommentProperties.PropertyService#getCacheTtlSeconds()}
     * controls eviction.
     */
    @Cacheable(value = "propertyMeta", unless = "#result == null")
    public PropertyMeta lookup(Long id) {
        try {
            JsonNode body = client.get()
                    .uri("/api/properties/{id}", id)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(props.getPropertyService().getTimeoutSeconds()));
            if (body == null) return PropertyMeta.MISSING;
            String ownerUserId = body.path("userId").asText(null);
            return new PropertyMeta(true, ownerUserId);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return PropertyMeta.MISSING;
            }
            log.warn("property-service lookup failed for id={}: {}", id, e.getStatusCode());
            // Fail-open: if upstream is sick, allow the write (we'll get the row anyway,
            // and an admin can clean up if it points to nothing).
            return new PropertyMeta(true, null);
        } catch (Exception e) {
            log.warn("property-service lookup error for id={}: {}", id, e.getMessage());
            return new PropertyMeta(true, null);
        }
    }
}
