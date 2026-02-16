package com.realestate.price.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PricePointResponse(
    BigDecimal oldPrice,
    BigDecimal newPrice,
    String changedBy,
    LocalDateTime changedAt,
    String reason
) {}
