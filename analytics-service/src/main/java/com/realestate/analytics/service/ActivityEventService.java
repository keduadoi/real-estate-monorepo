package com.realestate.analytics.service;

import com.realestate.analytics.dto.UserActivityEvent;
import com.realestate.analytics.entity.ActivityEvent;
import com.realestate.analytics.repository.ActivityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityEventService {

    private final ActivityEventRepository activityEventRepository;

    public void save(UserActivityEvent event) {
        ActivityEvent entity = ActivityEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())
                .timestamp(event.getTimestamp())
                .serviceName(event.getServiceName())
                .httpMethod(event.getHttpMethod())
                .endpoint(event.getEndpoint())
                .statusCode(event.getStatusCode())
                .durationMs(event.getDurationMs())
                .metadata(event.getMetadata())
                .build();

        activityEventRepository.save(entity);
        log.debug("Persisted activity event: eventId={}", event.getEventId());
    }
}
