package com.realestate.price.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PriceResponse(
    Long propertyId,
    BigDecimal currentPrice,
    String currency,
    String lastUpdatedBy,
    LocalDateTime updatedAt
) {}
