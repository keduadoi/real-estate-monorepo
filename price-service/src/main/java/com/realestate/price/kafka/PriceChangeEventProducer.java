package com.realestate.price.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceChangeEventProducer {

    private static final String TOPIC = "price-change-events";

    private final KafkaTemplate<String, PriceChangeEvent> kafkaTemplate;

    public void sendPriceChangeEvent(PriceChangeEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getPropertyId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to send price change event: propertyId={} error={}",
                                    event.getPropertyId(), ex.getMessage());
                        } else {
                            log.debug("Price change event sent: propertyId={} topic={} partition={}",
                                    event.getPropertyId(), TOPIC,
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            log.warn("Error sending price change event: propertyId={} error={}",
                    event.getPropertyId(), e.getMessage());
        }
    }
}
