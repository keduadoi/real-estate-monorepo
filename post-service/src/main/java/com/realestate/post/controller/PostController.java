package com.realestate.post.controller;

import com.realestate.post.dto.request.CreatePostRequest;
import com.realestate.post.dto.request.CreateReplyRequest;
import com.realestate.post.dto.request.UpdatePostRequest;
import com.realestate.post.dto.response.LikeResponse;
import com.realestate.post.dto.response.PageResponse;
import com.realestate.post.dto.response.PostResponse;
import com.realestate.post.dto.response.ReplyResponse;
import com.realestate.post.exception.UnauthorizedException;
import com.realestate.post.security.UserContext;
import com.realestate.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Post management endpoints")
public class PostController {

    private final PostService postService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get paginated feed of all posts
     */
    @GetMapping
    @Operation(summary = "Get feed", description = "Get paginated list of all posts, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feed retrieved successfully")
    })
    public ResponseEntity<PageResponse<PostResponse>> getFeed(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting feed: page={}, size={}", page, size);
        return ResponseEntity.ok(postService.getFeed(page, size));
    }

    /**
     * Get single post by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get post", description = "Get a single post by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<PostResponse> getPost(
            @Parameter(description = "Post ID")
            @PathVariable UUID id) {

        log.debug("Getting post: id={}", id);
        return ResponseEntity.ok(postService.getPost(id));
    }

    /**
     * Get posts by user ID
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user posts", description = "Get paginated posts by a specific user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    })
    public ResponseEntity<PageResponse<PostResponse>> getPostsByUser(
            @Parameter(description = "User ID")
            @PathVariable String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting posts for user: userId={}, page={}, size={}", userId, page, size);
        return ResponseEntity.ok(postService.getPostsByUser(userId, page, size));
    }

    /**
     * Search posts by content
     */
    @GetMapping("/search")
    @Operation(summary = "Search posts", description = "Search posts by content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search results returned")
    })
    public ResponseEntity<PageResponse<PostResponse>> searchPosts(
            @Parameter(description = "Search query")
            @RequestParam String q,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Searching posts: query={}, page={}, size={}", q, page, size);
        return ResponseEntity.ok(postService.searchPosts(q, page, size));
    }

    // ==================== AUTHENTICATED ENDPOINTS ====================

    /**
     * Create a new post
     */
    @PostMapping
    @Operation(summary = "Create post", description = "Create a new post (requires authentication)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request) {

        log.debug("Creating post: contentLength={}, images={}",
                request.content() != null ? request.content().length() : 0,
                request.imageUrls() != null ? request.imageUrls().size() : 0);
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a post
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update post", description = "Update a post (owner or admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update this post"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "Post ID")
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {

        log.debug("Updating post: id={}", id);
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    /**
     * Delete a post
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post", description = "Delete a post (owner or admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this post"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "Post ID")
            @PathVariable UUID id) {

        log.debug("Deleting post: id={}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle like on a post
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like", description = "Like or unlike a post (requires authentication)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Like toggled successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<LikeResponse> toggleLike(
            @Parameter(description = "Post ID")
            @PathVariable UUID id) {

        log.debug("Toggling like: postId={}", id);
        return ResponseEntity.ok(postService.toggleLike(id));
    }

    /**
     * Get paginated replies for a post
     */
    @GetMapping("/{id}/replies")
    @Operation(summary = "Get replies", description = "Get paginated replies for a post, oldest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Replies retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<PageResponse<ReplyResponse>> getReplies(
            @Parameter(description = "Post ID")
            @PathVariable UUID id,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting replies: postId={}, page={}, size={}", id, page, size);
        return ResponseEntity.ok(postService.getReplies(id, page, size));
    }

    /**
     * Reply to a post (single level — replies cannot be replied to)
     */
    @PostMapping("/{id}/replies")
    @Operation(summary = "Create reply", description = "Reply to a post (requires authentication)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reply created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ReplyResponse> createReply(
            @Parameter(description = "Post ID")
            @PathVariable UUID id,
            @Valid @RequestBody CreateReplyRequest request) {

        log.debug("Creating reply: postId={}", id);
        ReplyResponse response = postService.createReply(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Delete a reply
     */
    @DeleteMapping("/{id}/replies/{replyId}")
    @Operation(summary = "Delete reply", description = "Delete a reply (owner or admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reply deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete this reply"),
            @ApiResponse(responseCode = "404", description = "Reply not found")
    })
    public ResponseEntity<Void> deleteReply(
            @Parameter(description = "Post ID")
            @PathVariable UUID id,
            @Parameter(description = "Reply ID")
            @PathVariable UUID replyId) {

        log.debug("Deleting reply: postId={}, replyId={}", id, replyId);
        postService.deleteReply(id, replyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current user's posts
     */
    @GetMapping("/me")
    @Operation(summary = "Get my posts", description = "Get current authenticated user's posts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        String userId = UserContext.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        log.debug("Getting my posts: userId={}, page={}, size={}", userId, page, size);
        return ResponseEntity.ok(postService.getPostsByUser(userId, page, size));
    }

    /**
     * Check if post exists
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    @Operation(summary = "Check post exists", description = "Check if a post exists")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Post exists"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<Void> checkPostExists(
            @Parameter(description = "Post ID")
            @PathVariable UUID id) {

        postService.getPost(id); // Throws 404 if not found
        return ResponseEntity.ok().build();
    }
}
