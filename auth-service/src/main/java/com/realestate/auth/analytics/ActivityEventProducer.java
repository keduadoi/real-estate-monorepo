package com.realestate.auth.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActivityEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventProducer.class);

    private final KafkaTemplate<String, UserActivityEvent> kafkaTemplate;
    private final ActivityEventMappingConfig config;

    public ActivityEventProducer(KafkaTemplate<String, UserActivityEvent> kafkaTemplate,
                                 ActivityEventMappingConfig config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

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
