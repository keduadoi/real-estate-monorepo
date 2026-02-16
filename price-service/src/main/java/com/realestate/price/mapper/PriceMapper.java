package com.realestate.price.mapper;

import com.realestate.price.dto.PriceHistoryResponse;
import com.realestate.price.dto.PricePointResponse;
import com.realestate.price.dto.PriceResponse;
import com.realestate.price.entity.PriceHistory;
import com.realestate.price.entity.PropertyPrice;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceMapper {

    public PriceResponse toResponse(PropertyPrice entity) {
        return PriceResponse.builder()
                .propertyId(entity.getPropertyId())
                .currentPrice(entity.getCurrentPrice())
                .currency(entity.getCurrency())
                .lastUpdatedBy(entity.getLastUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PricePointResponse toPointResponse(PriceHistory history) {
        return PricePointResponse.builder()
                .oldPrice(history.getOldPrice())
                .newPrice(history.getNewPrice())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .reason(history.getReason())
                .build();
    }

    public PriceHistoryResponse toHistoryResponse(Long propertyId, List<PriceHistory> historyList) {
        List<PricePointResponse> points = historyList.stream()
                .map(this::toPointResponse)
                .toList();

        return PriceHistoryResponse.builder()
                .propertyId(propertyId)
                .pricePoints(points)
                .build();
    }
}
