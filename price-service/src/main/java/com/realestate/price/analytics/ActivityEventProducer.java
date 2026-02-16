package com.realestate.price.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityEventProducer {

    private final KafkaTemplate<String, UserActivityEvent> kafkaTemplate;
    private final ActivityEventMappingConfig config;

    public void sendEvent(UserActivityEvent event) {
        if (!config.getKafka().isEnabled()) {
            return;
        }

        try {
            String topic = config.getKafka().getTopic();
            kafkaTemplate.send(topic, event.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to send activity event: type={} error={}",
                                    event.getEventType(), ex.getMessage());
                        } else {
                            log.debug("Activity event sent: type={} topic={} partition={}",
                                    event.getEventType(), topic,
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error sending activity event: type={} error={}",
                    event.getEventType(), e.getMessage());
        }
    }
}
