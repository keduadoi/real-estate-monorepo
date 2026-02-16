package com.realestate.price.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record PriceHistoryResponse(
    Long propertyId,
    List<PricePointResponse> pricePoints
) {}
