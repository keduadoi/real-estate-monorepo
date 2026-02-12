package com.realestate.backend.analytics;

import com.realestate.backend.security.UserContext;
import com.realestate.backend.security.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityTrackingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "analytics.startTime";

    private final ActivityEventProducer producer;
    private final ActivityEventMappingConfig config;

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

            Optional<UserInfo> userOpt = UserContext.getCurrentUser();
            String userId = userOpt.map(UserInfo::getId).orElse("anonymous");
            String userEmail = userOpt.map(UserInfo::getEmail).orElse(null);

            Map<String, String> metadata = new HashMap<>();
            // Extract path variables from the actual URI
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
                    .userEmail(userEmail)
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
