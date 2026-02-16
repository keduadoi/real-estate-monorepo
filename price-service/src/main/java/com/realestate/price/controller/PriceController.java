package com.realestate.price.controller;

import com.realestate.price.dto.PriceHistoryResponse;
import com.realestate.price.dto.PriceResponse;
import com.realestate.price.dto.UpdatePriceRequest;
import com.realestate.price.service.PriceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
@Slf4j
public class PriceController {

    private final PriceManagementService priceService;

    @GetMapping("/{propertyId}")
    public ResponseEntity<PriceResponse> getCurrentPrice(@PathVariable Long propertyId) {
        log.debug("Getting current price for property: {}", propertyId);
        return ResponseEntity.ok(priceService.getCurrentPrice(propertyId));
    }

    @GetMapping("/{propertyId}/history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(@PathVariable Long propertyId) {
        log.debug("Getting price history for property: {}", propertyId);
        return ResponseEntity.ok(priceService.getPriceHistory(propertyId));
    }

    @PutMapping("/{propertyId}")
    public ResponseEntity<PriceResponse> updatePrice(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePriceRequest request) {
        log.debug("Updating price for property: {}", propertyId);
        return ResponseEntity.ok(priceService.updatePrice(propertyId, request));
    }
}
