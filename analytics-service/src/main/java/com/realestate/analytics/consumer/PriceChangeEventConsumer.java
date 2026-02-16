package com.realestate.analytics.consumer;

import com.realestate.analytics.dto.PriceChangeEvent;
import com.realestate.analytics.entity.PriceChangeRecord;
import com.realestate.analytics.repository.PriceChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceChangeEventConsumer {

    private final PriceChangeRepository priceChangeRepository;

    @KafkaListener(
            topics = "price-change-events",
            groupId = "${spring.kafka.consumer.group-id:analytics-service-group}",
            containerFactory = "priceChangeKafkaListenerContainerFactory"
    )
    public void consume(PriceChangeEvent event) {
        log.info("[PRICE_CHANGE] propertyId={} oldPrice={} newPrice={} currency={} changedBy={}",
                event.getPropertyId(),
                event.getOldPrice(),
                event.getNewPrice(),
                event.getCurrency(),
                event.getChangedBy());

        if (log.isDebugEnabled()) {
            log.debug("[PRICE_CHANGE_DETAIL] eventId={} changedAt={} reason={}",
                    event.getEventId(),
                    event.getChangedAt(),
                    event.getReason());
        }

        try {
            PriceChangeRecord record = PriceChangeRecord.builder()
                    .eventId(event.getEventId())
                    .propertyId(event.getPropertyId())
                    .oldPrice(event.getOldPrice())
                    .newPrice(event.getNewPrice())
                    .currency(event.getCurrency())
                    .changedBy(event.getChangedBy())
                    .changedAt(event.getChangedAt())
                    .reason(event.getReason())
                    .build();

            priceChangeRepository.save(record);
            log.debug("Persisted price change event: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to persist price change event: eventId={}, error={}",
                    event.getEventId(), e.getMessage(), e);
        }
    }
}
