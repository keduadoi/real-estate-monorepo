package com.realestate.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangeEvent {

    private String eventId;
    private Long propertyId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String currency;
    private String changedBy;
    private String changedAt;
    private String reason;
}
