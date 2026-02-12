package com.realestate.analytics.consumer;

import com.realestate.analytics.dto.UserActivityEvent;
import com.realestate.analytics.service.ActivityEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventConsumer {

    private final ActivityEventService activityEventService;

    @KafkaListener(
            topics = "${analytics.kafka.topic:user-activity-events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service-group}"
    )
    public void consume(UserActivityEvent event) {
        log.info("[ACTIVITY] type={} user={} service={} method={} endpoint={} status={} duration={}ms",
                event.getEventType(),
                event.getUserId(),
                event.getServiceName(),
                event.getHttpMethod(),
                event.getEndpoint(),
                event.getStatusCode(),
                event.getDurationMs());

        if (log.isDebugEnabled()) {
            log.debug("[ACTIVITY_DETAIL] eventId={} email={} timestamp={} metadata={}",
                    event.getEventId(),
                    event.getUserEmail(),
                    event.getTimestamp(),
                    event.getMetadata());
        }

        try {
            activityEventService.save(event);
        } catch (Exception e) {
            log.error("Failed to persist activity event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }
}
