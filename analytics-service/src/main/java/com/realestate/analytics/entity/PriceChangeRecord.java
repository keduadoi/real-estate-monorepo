package com.realestate.analytics.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "price_changes")
public class PriceChangeRecord {

    @Id
    private String id;

    private String eventId;
    private Long propertyId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String currency;
    private String changedBy;
    private String changedAt;
    private String reason;
}
