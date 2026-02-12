package com.realestate.auth.analytics;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ActivityTrackingInterceptor.class);
    private static final String START_TIME_ATTR = "analytics.startTime";

    private final ActivityEventProducer producer;
    private final ActivityEventMappingConfig config;

    public ActivityTrackingInterceptor(ActivityEventProducer producer,
                                       ActivityEventMappingConfig config) {
        this.producer = producer;
        this.config = config;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            if (!config.getKafka().isEnabled()) {
                return;
            }

            String method = request.getMethod();
            Object patternAttr = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String pattern = patternAttr != null ? patternAttr.toString() : null;
            if (pattern == null) {
                return;
            }

            String mappingKey = method + " " + pattern;
            String eventType = config.getEventMappings().get(mappingKey);
            if (eventType == null) {
                return;
            }

            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long durationMs = startTime != null ? System.currentTimeMillis() - startTime : 0;

            // Extract user from Spring Security context
            String userId = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID) {
                userId = auth.getPrincipal().toString();
            }

            Map<String, String> metadata = new HashMap<>();
            String requestUri = request.getRequestURI();
            if (!requestUri.equals(pattern)) {
                metadata.put("path", requestUri);
            }
            String queryString = request.getQueryString();
            if (queryString != null) {
                metadata.put("queryParams", queryString);
            }

            UserActivityEvent event = UserActivityEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .userId(userId)
                    .userEmail(null)
                    .timestamp(Instant.now().toString())
                    .serviceName(config.getServiceName())
                    .httpMethod(method)
                    .endpoint(requestUri)
                    .statusCode(response.getStatus())
                    .durationMs(durationMs)
                    .metadata(metadata)
                    .build();

            producer.sendEvent(event);
        } catch (Exception e) {
            log.warn("Error in activity tracking interceptor: {}", e.getMessage());
        }
    }
}
