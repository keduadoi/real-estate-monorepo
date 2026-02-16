package com.realestate.price.service;

import com.realestate.price.dto.PriceHistoryResponse;
import com.realestate.price.dto.PriceResponse;
import com.realestate.price.dto.UpdatePriceRequest;
import com.realestate.price.entity.PriceHistory;
import com.realestate.price.entity.PropertyPrice;
import com.realestate.price.exception.PriceNotFoundException;
import com.realestate.price.exception.ServiceUnavailableException;
import com.realestate.price.exception.UnauthorizedException;
import com.realestate.price.kafka.PriceChangeEvent;
import com.realestate.price.kafka.PriceChangeEventProducer;
import com.realestate.price.mapper.PriceMapper;
import com.realestate.price.repository.PriceHistoryRepository;
import com.realestate.price.repository.PropertyPriceRepository;
import com.realestate.price.security.UserContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PriceManagementService {

    private final PropertyPriceRepository priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final PriceChangeEventProducer eventProducer;
    private final PriceMapper priceMapper;

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "getCurrentPriceFallback")
    public PriceResponse getCurrentPrice(Long propertyId) {
        PropertyPrice price = priceRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new PriceNotFoundException(propertyId));

        return priceMapper.toResponse(price);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "getPriceHistoryFallback")
    public PriceHistoryResponse getPriceHistory(Long propertyId) {
        if (!priceRepository.existsByPropertyId(propertyId)) {
            throw new PriceNotFoundException(propertyId);
        }

        List<PriceHistory> history = historyRepository.findByPropertyIdOrderByChangedAtDesc(propertyId);
        return priceMapper.toHistoryResponse(propertyId, history);
    }

    public PriceResponse updatePrice(Long propertyId, UpdatePriceRequest request) {
        String userId = UserContext.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        return updatePriceInternal(propertyId, request.newPrice(),
                request.currency(), userId, request.reason());
    }

    public PriceResponse updatePriceInternal(Long propertyId, BigDecimal newPrice,
                                              String currency, String changedBy, String reason) {
        PropertyPrice price = priceRepository.findByPropertyId(propertyId).orElse(null);

        BigDecimal oldPrice = null;
        String effectiveCurrency = currency != null ? currency : "VND";

        if (price == null) {
            price = PropertyPrice.builder()
                    .propertyId(propertyId)
                    .currentPrice(newPrice)
                    .currency(effectiveCurrency)
                    .lastUpdatedBy(changedBy)
                    .build();
        } else {
            oldPrice = price.getCurrentPrice();
            price.setCurrentPrice(newPrice);
            price.setCurrency(effectiveCurrency);
            price.setLastUpdatedBy(changedBy);
        }

        PropertyPrice saved = priceRepository.save(price);

        PriceHistory history = PriceHistory.builder()
                .propertyId(propertyId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        historyRepository.save(history);

        PriceChangeEvent event = PriceChangeEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .propertyId(propertyId)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .currency(effectiveCurrency)
                .changedBy(changedBy)
                .changedAt(Instant.now().toString())
                .reason(reason)
                .build();
        eventProducer.sendPriceChangeEvent(event);

        log.info("Price updated: propertyId={}, oldPrice={}, newPrice={}, by={}",
                propertyId, oldPrice, newPrice, changedBy);

        return priceMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "batchGetPricesFallback")
    public List<PriceResponse> batchGetPrices(List<Long> propertyIds) {
        return priceRepository.findByPropertyIdIn(propertyIds).stream()
                .map(priceMapper::toResponse)
                .toList();
    }

    // Circuit breaker fallbacks

    private PriceResponse getCurrentPriceFallback(Long propertyId, Throwable t) {
        if (t instanceof PriceNotFoundException) {
            throw (PriceNotFoundException) t;
        }
        log.error("Database circuit breaker open — getCurrentPrice fallback for property {}: {}",
                propertyId, t.getMessage());
        throw new ServiceUnavailableException("Price service is temporarily unavailable. Please try again later.");
    }

    private PriceHistoryResponse getPriceHistoryFallback(Long propertyId, Throwable t) {
        if (t instanceof PriceNotFoundException) {
            throw (PriceNotFoundException) t;
        }
        log.error("Database circuit breaker open — getPriceHistory fallback for property {}: {}",
                propertyId, t.getMessage());
        throw new ServiceUnavailableException("Price service is temporarily unavailable. Please try again later.");
    }

    private List<PriceResponse> batchGetPricesFallback(List<Long> propertyIds, Throwable t) {
        log.error("Database circuit breaker open — batchGetPrices fallback: {}", t.getMessage());
        throw new ServiceUnavailableException("Price service is temporarily unavailable. Please try again later.");
    }
}
