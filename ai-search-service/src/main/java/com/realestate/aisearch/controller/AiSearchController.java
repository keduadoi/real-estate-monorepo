package com.realestate.aisearch.controller;

import com.realestate.aisearch.dto.AiSearchParseRequest;
import com.realestate.aisearch.dto.AiSearchParseResponse;
import com.realestate.aisearch.service.AiSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-search")
@RequiredArgsConstructor
@Slf4j
public class AiSearchController {

    private final AiSearchService service;

    @PostMapping("/parse")
    public ResponseEntity<AiSearchParseResponse> parse(@Valid @RequestBody AiSearchParseRequest request) {
        AiSearchParseResponse response = service.parse(request.query(), request.locale());
        return ResponseEntity.ok(response);
    }
}
