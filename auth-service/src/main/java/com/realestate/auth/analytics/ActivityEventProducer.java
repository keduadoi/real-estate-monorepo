package com.realestate.auth.analytics;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class ActivityEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ActivityEventProducer.class);

    private final KafkaTemplate<String, UserActivityEvent> kafkaTemplate;
    private final ActivityEventMappingConfig config;

    // Dedicated daemon thread so request threads never block on the Kafka client:
    // even KafkaTemplate.send() blocks up to max.block.ms when the broker is down.
    // Bounded queue with discard-oldest keeps memory flat during long outages.
    private final ExecutorService eventExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1000),
            runnable -> {
                Thread thread = new Thread(runnable, "activity-event-sender");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy());

    public ActivityEventProducer(KafkaTemplate<String, UserActivityEvent> kafkaTemplate,
                                 ActivityEventMappingConfig config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }

    public void sendEvent(UserActivityEvent event) {
        if (!config.getKafka().isEnabled()) {
            return;
        }

        eventExecutor.execute(() -> {
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
        });
    }

    @PreDestroy
    void shutdown() {
        eventExecutor.shutdown();
    }
}
