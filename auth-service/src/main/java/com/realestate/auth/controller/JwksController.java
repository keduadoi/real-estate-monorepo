package com.realestate.auth.controller;

import com.realestate.auth.dto.response.JwksResponse;
import com.realestate.auth.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResponse> getJwks() {
        return ResponseEntity.ok(jwtService.getJwks());
    }
}
