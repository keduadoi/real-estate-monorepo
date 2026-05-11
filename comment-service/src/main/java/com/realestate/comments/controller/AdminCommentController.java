package com.realestate.comments.controller;

import com.realestate.comments.dto.CommentResponse;
import com.realestate.comments.dto.HideCommentRequest;
import com.realestate.comments.dto.PagedResponse;
import com.realestate.comments.exception.ForbiddenException;
import com.realestate.comments.security.UserContext;
import com.realestate.comments.security.UserContextHolder;
import com.realestate.comments.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService service;

    @GetMapping
    public ResponseEntity<PagedResponse<CommentResponse>> listReported(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage) {
        UserContext viewer = requireAdmin();
        return ResponseEntity.ok(service.adminListReported(page, perPage, viewer));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<Void> hide(@PathVariable Long id,
                                     @Valid @RequestBody HideCommentRequest body) {
        UserContext admin = requireAdmin();
        service.hide(id, body.reason(), admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long id) {
        requireAdmin();
        service.restore(id);
        return ResponseEntity.noContent().build();
    }

    private UserContext requireAdmin() {
        UserContext ctx = UserContextHolder.get();
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Admin role required");
        }
        return ctx;
    }
}
