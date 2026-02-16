package com.realestate.backend.grpc;

import com.realestate.grpc.price.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PriceGrpcClient {

    @GrpcClient("price-service")
    private PriceServiceGrpc.PriceServiceBlockingStub priceServiceStub;

    @CircuitBreaker(name = "priceService", fallbackMethod = "getCurrentPriceFallback")
    public Optional<BigDecimal> getCurrentPrice(Long propertyId) {
        log.debug("gRPC: Getting current price for property {}", propertyId);
        try {
            Price response = priceServiceStub.getCurrentPrice(
                    PropertyId.newBuilder().setPropertyId(propertyId).build()
            );
            return Optional.of(new BigDecimal(response.getCurrentPrice()));
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.debug("No price record found in price-service for property {}", propertyId);
                return Optional.empty();
            }
            throw e;
        }
    }

    @CircuitBreaker(name = "priceService", fallbackMethod = "batchGetPricesFallback")
    public Map<Long, BigDecimal> batchGetPrices(List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.debug("gRPC: Batch getting prices for {} properties", propertyIds.size());

        BatchGetPricesResponse response = priceServiceStub.batchGetPrices(
                BatchGetPricesRequest.newBuilder()
                        .addAllPropertyIds(propertyIds)
                        .build()
        );

        return response.getPricesList().stream()
                .collect(Collectors.toMap(
                        Price::getPropertyId,
                        p -> new BigDecimal(p.getCurrentPrice())
                ));
    }

    @CircuitBreaker(name = "priceService", fallbackMethod = "updatePriceFallback")
    public void updatePrice(Long propertyId, BigDecimal newPrice, String changedBy, String reason) {
        log.debug("gRPC: Updating price for property {} to {}", propertyId, newPrice);

        UpdatePriceRequest.Builder builder = UpdatePriceRequest.newBuilder()
                .setPropertyId(propertyId)
                .setNewPrice(newPrice.toPlainString())
                .setCurrency("VND");

        if (changedBy != null) {
            builder.setChangedBy(changedBy);
        }
        if (reason != null) {
            builder.setReason(reason);
        }

        priceServiceStub.updatePrice(builder.build());
        log.info("gRPC: Price updated successfully for property {}", propertyId);
    }

    // Fallback methods

    private Optional<BigDecimal> getCurrentPriceFallback(Long propertyId, Throwable t) {
        log.warn("Price service unavailable for property {} — using cached price. Error: {}", propertyId, t.getMessage());
        return Optional.empty();
    }

    private Map<Long, BigDecimal> batchGetPricesFallback(List<Long> propertyIds, Throwable t) {
        log.warn("Price service unavailable for batch request ({} properties) — using cached prices. Error: {}",
                propertyIds.size(), t.getMessage());
        return Collections.emptyMap();
    }

    private void updatePriceFallback(Long propertyId, BigDecimal newPrice, String changedBy, String reason, Throwable t) {
        log.error("Price service unavailable — cannot update price for property {}. Error: {}", propertyId, t.getMessage());
        throw new com.realestate.backend.exception.ServiceUnavailableException(
                "Price service is temporarily unavailable. Price update failed for property " + propertyId);
    }
}
