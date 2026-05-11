package com.realestate.comments.controller;

import com.realestate.comments.dto.*;
import com.realestate.comments.security.UserContextHolder;
import com.realestate.comments.service.CaptchaService;
import com.realestate.comments.service.CommentService;
import com.realestate.comments.service.IpHasher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService service;
    private final CaptchaService captcha;
    private final IpHasher ipHasher;

    // ---- List ---------------------------------------------------------------

    @GetMapping("/api/properties/{propertyId}/comments")
    public ResponseEntity<PagedResponse<CommentResponse>> list(
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int perPage) {
        return ResponseEntity.ok(service.listForProperty(propertyId, page, perPage,
                UserContextHolder.get()));
    }

    // ---- Captcha ------------------------------------------------------------

    @GetMapping("/api/captcha")
    public ResponseEntity<CaptchaChallenge> captcha() {
        return ResponseEntity.ok(captcha.issue());
    }

    // ---- Create top-level ---------------------------------------------------

    @PostMapping("/api/properties/{propertyId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long propertyId,
            @Valid @RequestBody CreateCommentRequest body,
            HttpServletRequest request) {
        String ipHash = ipHasher.hash(ipHasher.extractClientIp(request));
        CommentResponse created = service.create(
                propertyId,
                body.sanitized(),
                UserContextHolder.get(),
                ipHash,
                request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ---- Create reply -------------------------------------------------------

    @PostMapping("/api/comments/{parentId}/reply")
    public ResponseEntity<CommentResponse> reply(
            @PathVariable Long parentId,
            @Valid @RequestBody CreateCommentRequest body,
            HttpServletRequest request) {
        String ipHash = ipHasher.hash(ipHasher.extractClientIp(request));
        CommentResponse created = service.reply(
                parentId,
                body.sanitized(),
                UserContextHolder.get(),
                ipHash,
                request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ---- Edit ---------------------------------------------------------------

    @PutMapping("/api/comments/{id}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest body) {
        return ResponseEntity.ok(service.update(id, body, UserContextHolder.get()));
    }

    // ---- Delete -------------------------------------------------------------

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, UserContextHolder.get());
        return ResponseEntity.noContent().build();
    }

    // ---- Like / report ------------------------------------------------------

    @PostMapping("/api/comments/{id}/like")
    public ResponseEntity<Map<String, Object>> like(
            @PathVariable Long id,
            HttpServletRequest request) {
        String ipHash = ipHasher.hash(ipHasher.extractClientIp(request));
        boolean liked = service.toggleLike(id, UserContextHolder.get(), ipHash);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @PostMapping("/api/comments/{id}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long id,
            @Valid @RequestBody ReportCommentRequest body,
            HttpServletRequest request) {
        String ipHash = ipHasher.hash(ipHasher.extractClientIp(request));
        service.report(id, body, UserContextHolder.get(), ipHash);
        return ResponseEntity.accepted().build();
    }
}
