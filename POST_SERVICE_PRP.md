# Product Requirements Proposal (PRP)
# Post Service - Social Feed Feature for Real Estate Application

**Version**: 1.0
**Date**: 2026-01-29
**Author**: Engineering Team
**Status**: Draft - Pending Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Objectives](#3-goals-and-objectives)
4. [Technical Architecture](#4-technical-architecture)
5. [Database Schema & Migrations](#5-database-schema--migrations)
6. [Entity Classes](#6-entity-classes)
7. [Repository Layer](#7-repository-layer)
8. [Service Layer](#8-service-layer)
9. [Controller Layer (API Endpoints)](#9-controller-layer-api-endpoints)
10. [DTOs (Request/Response)](#10-dtos-requestresponse)
11. [Security & Kong Integration](#11-security--kong-integration)
12. [Frontend Integration](#12-frontend-integration)
13. [Exception Handling](#13-exception-handling)
14. [Configuration](#14-configuration)
15. [Testing Strategy](#15-testing-strategy)
16. [Deployment](#16-deployment)
17. [Implementation Plan](#17-implementation-plan)
18. [Success Metrics](#18-success-metrics)

---

## 1. Executive Summary

### 1.1 Overview

This PRP outlines the implementation of a dedicated **Post Service** microservice to handle social feed functionality in the Real Estate application. The service will manage user posts, likes, and social interactions, replacing the current frontend mock data implementation.

### 1.2 Current State

```
Frontend (Mock Data)
├── mockData.ts contains hardcoded posts and likes
├── API routes (/api/posts) return mock data
├── No persistence - data resets on refresh
└── No real user association
```

### 1.3 Target State

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Frontend  │────▶│ Kong Gateway │────▶│   Post Service   │
│  (Next.js)  │     │  (JWT Auth)  │     │  (Spring Boot)   │
└─────────────┘     └──────────────┘     └──────────────────┘
                                                  │
                           ┌──────────────────────┼──────────────────────┐
                           ▼                      ▼                      ▼
                    ┌────────────┐         ┌────────────┐         ┌────────────┐
                    │ PostgreSQL │         │Auth Service│         │  Backend   │
                    │  (Posts)   │         │  (Users)   │         │(Properties)│
                    └────────────┘         └────────────┘         └────────────┘
```

### 1.4 Key Benefits

| Benefit | Description |
|---------|-------------|
| **Separation of Concerns** | Posts/social features isolated from property service |
| **Independent Scaling** | Social features can scale based on different load patterns |
| **Technology Flexibility** | Can optimize for high-read workloads independently |
| **Feature Evolution** | Easy to add comments, shares, notifications without affecting other services |

---

## 2. Problem Statement

### 2.1 Current Limitations

1. **No Data Persistence**: Posts exist only in frontend memory, lost on refresh
2. **No Real User Association**: Mock users don't connect to actual authenticated users
3. **No Cross-User Visibility**: Users cannot see posts from other real users
4. **Limited Scalability**: Frontend mock data cannot scale for production
5. **No Analytics**: Cannot track engagement metrics

### 2.2 User Stories

| ID | Story | Priority |
|----|-------|----------|
| US-1 | As a user, I want to create posts that persist after page refresh | High |
| US-2 | As a user, I want to see posts from all users in my feed | High |
| US-3 | As a user, I want to like/unlike posts from other users | High |
| US-4 | As a user, I want to see who liked my posts | Medium |
| US-5 | As a user, I want to delete my own posts | High |
| US-6 | As a user, I want to edit my own posts | Medium |
| US-7 | As a user, I want to see posts with the author's profile info | High |
| US-8 | As an admin, I want to moderate/delete inappropriate posts | Low |

---

## 3. Goals and Objectives

### 3.1 Primary Goals

1. **Implement persistent post storage** with PostgreSQL database
2. **Integrate with existing auth-service** for user identity
3. **Provide RESTful API** following existing patterns (property service)
4. **Support like/unlike functionality** with real-time counts
5. **Enable pagination and sorting** for feed scalability

### 3.2 Non-Goals (Out of Scope for v1)

- Comments on posts
- Post sharing/reposting
- Image/media attachments in posts
- Real-time notifications (WebSocket)
- Post analytics dashboard
- Hashtags and mentions

### 3.3 Success Criteria

- [ ] Posts persist across page refreshes and sessions
- [ ] Posts display real user names from auth-service
- [ ] Like counts update correctly without duplicates
- [ ] API response times < 200ms for feed queries
- [ ] 95%+ test coverage on service layer

---

## 4. Technical Architecture

### 4.1 Service Architecture

```
post-service/
├── pom.xml                              # Spring Boot 3.x dependencies
├── Dockerfile                           # Multi-stage build
├── src/main/java/com/realestate/post/
│   ├── PostServiceApplication.java
│   ├── config/
│   │   ├── WebConfig.java               # CORS configuration
│   │   └── OpenApiConfig.java           # Swagger documentation
│   ├── controller/
│   │   └── PostController.java          # REST endpoints
│   ├── service/
│   │   ├── PostService.java             # Business logic
│   │   └── UserInfoService.java         # Fetch user info from auth-service
│   ├── repository/
│   │   ├── PostRepository.java
│   │   └── LikeRepository.java
│   ├── entity/
│   │   ├── Post.java
│   │   └── Like.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreatePostRequest.java
│   │   │   └── UpdatePostRequest.java
│   │   └── response/
│   │       ├── PostResponse.java
│   │       ├── PostAuthorResponse.java
│   │       └── PageResponse.java
│   ├── security/
│   │   ├── UserContext.java             # ThreadLocal user info
│   │   ├── UserInfo.java                # User details from Kong headers
│   │   └── UserContextFilter.java       # Extract user from headers
│   ├── exception/
│   │   ├── PostNotFoundException.java
│   │   ├── UnauthorizedException.java
│   │   ├── ForbiddenException.java
│   │   └── GlobalExceptionHandler.java
│   └── client/
│       └── AuthServiceClient.java       # Feign client for auth-service
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__create_posts_table.sql
│       └── V2__create_likes_table.sql
└── src/test/java/
    └── com/realestate/post/
        ├── controller/
        ├── service/
        └── repository/
```

### 4.2 Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| spring-boot-starter-web | 3.2.x | REST API |
| spring-boot-starter-data-jpa | 3.2.x | Database access |
| spring-boot-starter-validation | 3.2.x | Input validation |
| spring-cloud-starter-openfeign | 4.1.x | Auth-service communication |
| flyway-core | 10.x | Database migrations |
| postgresql | 42.x | Database driver |
| spring-boot-starter-actuator | 3.2.x | Health checks |
| springdoc-openapi-starter-webmvc-ui | 2.3.x | API documentation |
| micrometer-registry-prometheus | 1.12.x | Metrics |

### 4.3 Inter-Service Communication

```
┌──────────────────────────────────────────────────────────────────┐
│                         Kong Gateway                              │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Route: /api/posts/* → post-service:8082                     │ │
│  │ Plugin: jwt (validates token, adds X-User-* headers)        │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  Auth Service   │  │  Post Service   │  │ Property Service│
│   (port 8081)   │  │   (port 8082)   │  │   (port 8080)   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │
         │     ┌──────────────┘
         │     │ Internal call for user info
         │     │ GET /internal/users/{id}
         ▼     ▼
┌─────────────────┐
│  Auth Service   │
│ /internal/users │
└─────────────────┘
```

---

## 5. Database Schema & Migrations

### 5.1 Database: `postdb`

Separate database from auth and property services for isolation.

### 5.2 Migrations

**V1__create_posts_table.sql**
```sql
CREATE TABLE posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content         TEXT NOT NULL,
    user_id         VARCHAR(36) NOT NULL,  -- Reference to auth-service user UUID
    author_name     VARCHAR(255),          -- Denormalized for display performance
    author_email    VARCHAR(255),          -- Denormalized for display performance
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);
```

**V2__create_likes_table.sql**
```sql
CREATE TABLE likes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id         VARCHAR(36) NOT NULL,  -- Reference to auth-service user UUID
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_post_user_like UNIQUE (post_id, user_id)
);

CREATE INDEX idx_likes_post_id ON likes(post_id);
CREATE INDEX idx_likes_user_id ON likes(user_id);
```

### 5.3 Schema Diagram

```
┌─────────────────────────────────────┐
│              posts                   │
├─────────────────────────────────────┤
│ id           UUID PK                │
│ content      TEXT NOT NULL          │
│ user_id      VARCHAR(36) NOT NULL   │──────┐
│ author_name  VARCHAR(255)           │      │
│ author_email VARCHAR(255)           │      │
│ created_at   TIMESTAMPTZ            │      │
│ updated_at   TIMESTAMPTZ            │      │
└─────────────────────────────────────┘      │
         │                                    │
         │ 1:N                                │
         ▼                                    │
┌─────────────────────────────────────┐      │
│              likes                   │      │
├─────────────────────────────────────┤      │
│ id           UUID PK                │      │
│ post_id      UUID FK ───────────────│──────┘
│ user_id      VARCHAR(36) NOT NULL   │
│ created_at   TIMESTAMPTZ            │
│ UNIQUE(post_id, user_id)            │
└─────────────────────────────────────┘
```

---

## 6. Entity Classes

### 6.1 Post Entity

```java
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 1, max = 5000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @NotBlank
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    // Getters, setters, equals, hashCode
}
```

### 6.2 Like Entity

```java
@Entity
@Table(name = "likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "user_id"})
})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @NotBlank
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Getters, setters, equals, hashCode
}
```

---

## 7. Repository Layer

### 7.1 PostRepository

```java
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // Find posts ordered by creation date (feed)
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Find posts by specific user
    Page<Post> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Count posts by user
    long countByUserId(String userId);

    // Search posts by content
    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Page<Post> searchByContent(@Param("query") String query, Pageable pageable);
}
```

### 7.2 LikeRepository

```java
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    // Check if user liked a post
    boolean existsByPostIdAndUserId(UUID postId, String userId);

    // Find like by post and user
    Optional<Like> findByPostIdAndUserId(UUID postId, String userId);

    // Count likes for a post
    long countByPostId(UUID postId);

    // Get all likes for a post
    List<Like> findByPostId(UUID postId);

    // Get all likes by a user
    List<Like> findByUserId(String userId);

    // Delete like by post and user
    void deleteByPostIdAndUserId(UUID postId, String userId);

    // Batch count likes for multiple posts (performance optimization)
    @Query("SELECT l.post.id, COUNT(l) FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countLikesByPostIds(@Param("postIds") List<UUID> postIds);
}
```

---

## 8. Service Layer

### 8.1 PostService

```java
@Service
@Transactional
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserInfoService userInfoService;

    // Create a new post
    public PostResponse createPost(CreatePostRequest request) {
        String userId = UserContext.getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        UserInfo currentUser = UserContext.getCurrentUser()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = new Post();
        post.setContent(request.getContent());
        post.setUserId(userId);
        post.setAuthorName(currentUser.getFullName());  // Denormalized
        post.setAuthorEmail(currentUser.getEmail());    // Denormalized

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, userId={}", saved.getId(), userId);

        return mapToResponse(saved, userId);
    }

    // Get paginated feed (all posts)
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getFeed(int page, int size) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostResponse> responses = posts.getContent().stream()
            .map(post -> mapToResponse(post, currentUserId))
            .toList();

        return new PageResponse<>(
            responses,
            posts.getTotalElements(),
            posts.getNumber(),
            posts.getSize(),
            posts.getTotalPages()
        );
    }

    // Get single post by ID
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        return mapToResponse(post, currentUserId);
    }

    // Get posts by user
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByUser(String userId, int page, int size) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PostResponse> responses = posts.getContent().stream()
            .map(post -> mapToResponse(post, currentUserId))
            .toList();

        return new PageResponse<>(responses, posts.getTotalElements(),
            posts.getNumber(), posts.getSize(), posts.getTotalPages());
    }

    // Update post (owner only)
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        String userId = UserContext.getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("You can only edit your own posts");
        }

        post.setContent(request.getContent());
        Post updated = postRepository.save(post);

        log.info("Post updated: id={}, userId={}", postId, userId);
        return mapToResponse(updated, userId);
    }

    // Delete post (owner or admin)
    public void deletePost(UUID postId) {
        String userId = UserContext.getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("You can only delete your own posts");
        }

        postRepository.delete(post);  // Cascades to likes
        log.info("Post deleted: id={}, userId={}", postId, userId);
    }

    // Toggle like (like if not liked, unlike if liked)
    public LikeResponse toggleLike(UUID postId) {
        String userId = UserContext.getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        Optional<Like> existingLike = likeRepository.findByPostIdAndUserId(postId, userId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            isLiked = false;
            log.info("Post unliked: postId={}, userId={}", postId, userId);
        } else {
            Like like = new Like();
            like.setPost(post);
            like.setUserId(userId);
            likeRepository.save(like);
            isLiked = true;
            log.info("Post liked: postId={}, userId={}", postId, userId);
        }

        long likeCount = likeRepository.countByPostId(postId);
        return new LikeResponse(isLiked, likeCount);
    }

    // Map entity to response DTO
    private PostResponse mapToResponse(Post post, String currentUserId) {
        long likeCount = likeRepository.countByPostId(post.getId());
        boolean isLikedByCurrentUser = currentUserId != null &&
            likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return PostResponse.builder()
            .id(post.getId())
            .content(post.getContent())
            .author(PostAuthorResponse.builder()
                .id(post.getUserId())
                .name(post.getAuthorName())
                .email(post.getAuthorEmail())
                .build())
            .likeCount(likeCount)
            .isLikedByCurrentUser(isLikedByCurrentUser)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
```

### 8.2 UserInfoService (For fetching user details from auth-service)

```java
@Service
@Slf4j
public class UserInfoService {

    private final AuthServiceClient authServiceClient;

    // Fetch user info from auth-service (for cases where we need fresh data)
    public Optional<UserInfoResponse> getUserInfo(String userId) {
        try {
            return Optional.of(authServiceClient.getUserById(userId));
        } catch (FeignException.NotFound e) {
            log.warn("User not found in auth-service: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching user info: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // Batch fetch user info (for feed optimization)
    public Map<String, UserInfoResponse> getUserInfoBatch(List<String> userIds) {
        try {
            return authServiceClient.getUsersByIds(userIds);
        } catch (Exception e) {
            log.error("Error batch fetching user info: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
```

---

## 9. Controller Layer (API Endpoints)

### 9.1 PostController

```java
@RestController
@RequestMapping("/api/posts")
@Slf4j
@Tag(name = "Posts", description = "Post management endpoints")
public class PostController {

    private final PostService postService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get paginated feed of all posts
     * GET /api/posts?page=0&size=20
     */
    @GetMapping
    @Operation(summary = "Get feed", description = "Get paginated list of all posts")
    public ResponseEntity<PageResponse<PostResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(postService.getFeed(page, size));
    }

    /**
     * Get single post by ID
     * GET /api/posts/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get post", description = "Get a single post by ID")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    /**
     * Get posts by user ID
     * GET /api/posts/user/{userId}?page=0&size=20
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user posts", description = "Get paginated posts by a specific user")
    public ResponseEntity<PageResponse<PostResponse>> getPostsByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(postService.getPostsByUser(userId, page, size));
    }

    // ==================== AUTHENTICATED ENDPOINTS ====================

    /**
     * Create a new post
     * POST /api/posts
     * Requires: Authentication
     */
    @PostMapping
    @Operation(summary = "Create post", description = "Create a new post (requires authentication)")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request) {

        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a post
     * PUT /api/posts/{id}
     * Requires: Authentication, ownership or admin
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update post", description = "Update a post (owner or admin only)")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request) {

        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    /**
     * Delete a post
     * DELETE /api/posts/{id}
     * Requires: Authentication, ownership or admin
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post", description = "Delete a post (owner or admin only)")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle like on a post
     * POST /api/posts/{id}/like
     * Requires: Authentication
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like", description = "Like or unlike a post")
    public ResponseEntity<LikeResponse> toggleLike(@PathVariable UUID id) {
        return ResponseEntity.ok(postService.toggleLike(id));
    }

    /**
     * Get current user's posts
     * GET /api/posts/me?page=0&size=20
     * Requires: Authentication
     */
    @GetMapping("/me")
    @Operation(summary = "Get my posts", description = "Get current user's posts")
    public ResponseEntity<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = UserContext.getCurrentUserId()
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        return ResponseEntity.ok(postService.getPostsByUser(userId, page, size));
    }
}
```

### 9.2 API Endpoints Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/posts` | Optional | Get paginated feed |
| GET | `/api/posts/{id}` | Optional | Get single post |
| GET | `/api/posts/user/{userId}` | Optional | Get posts by user |
| GET | `/api/posts/me` | Required | Get current user's posts |
| POST | `/api/posts` | Required | Create new post |
| PUT | `/api/posts/{id}` | Required | Update post (owner/admin) |
| DELETE | `/api/posts/{id}` | Required | Delete post (owner/admin) |
| POST | `/api/posts/{id}/like` | Required | Toggle like |

---

## 10. DTOs (Request/Response)

### 10.1 Request DTOs

```java
// CreatePostRequest.java
public record CreatePostRequest(
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
    String content
) {}

// UpdatePostRequest.java
public record UpdatePostRequest(
    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
    String content
) {}
```

### 10.2 Response DTOs

```java
// PostResponse.java
@Builder
public record PostResponse(
    UUID id,
    String content,
    PostAuthorResponse author,
    long likeCount,
    boolean isLikedByCurrentUser,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// PostAuthorResponse.java
@Builder
public record PostAuthorResponse(
    String id,
    String name,
    String email
) {}

// LikeResponse.java
public record LikeResponse(
    boolean liked,
    long likeCount
) {}

// PageResponse.java (shared)
public record PageResponse<T>(
    List<T> data,
    long total,
    int page,
    int perPage,
    int totalPages
) {}
```

---

## 11. Security & Kong Integration

### 11.1 Kong Route Configuration

Add to existing Kong configuration:

```yaml
services:
  - name: post-service
    url: http://post-service:8082
    routes:
      - name: post-routes
        paths:
          - /api/posts
        strip_path: false
    plugins:
      - name: jwt
        config:
          uri_param_names: []
          cookie_names: []
          key_claim_name: kid
          claims_to_verify:
            - exp
      - name: request-transformer
        config:
          add:
            headers:
              - "X-User-Id:$(jwt.sub)"
              - "X-User-Email:$(jwt.email)"
              - "X-User-Roles:$(jwt.roles)"
              - "X-User-Name:$(jwt.name)"
```

### 11.2 Security Classes (Reuse from Backend)

```java
// UserContext.java
public class UserContext {
    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(UserInfo user) {
        CURRENT_USER.set(user);
    }

    public static Optional<UserInfo> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static Optional<String> getCurrentUserId() {
        return getCurrentUser().map(UserInfo::getId);
    }

    public static boolean isAuthenticated() {
        return getCurrentUser().map(UserInfo::isAuthenticated).orElse(false);
    }

    public static boolean isAdmin() {
        return getCurrentUser().map(UserInfo::isAdmin).orElse(false);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}

// UserInfo.java
@Data
@Builder
public class UserInfo {
    private String id;
    private String email;
    private String name;
    private String rolesString;

    public List<String> getRoles() {
        return rolesString != null
            ? Arrays.asList(rolesString.split(","))
            : Collections.emptyList();
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    public boolean isAuthenticated() {
        return id != null && !id.isEmpty();
    }

    public String getFullName() {
        return name != null ? name : email;
    }
}

// UserContextFilter.java
@Component
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";
    private static final String HEADER_USER_NAME = "X-User-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            String userId = request.getHeader(HEADER_USER_ID);
            String email = request.getHeader(HEADER_USER_EMAIL);
            String roles = request.getHeader(HEADER_USER_ROLES);
            String name = request.getHeader(HEADER_USER_NAME);

            if (userId != null && !userId.isEmpty()) {
                UserInfo userInfo = UserInfo.builder()
                    .id(userId)
                    .email(email)
                    .name(name)
                    .rolesString(roles)
                    .build();
                UserContext.setCurrentUser(userInfo);
            }

            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
```

---

## 12. Frontend Integration

### 12.1 API Client

Create `frontend/lib/api/postApi.ts`:

```typescript
import { getSession } from 'next-auth/react';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8000';

export interface Post {
  id: string;
  content: string;
  author: {
    id: string;
    name: string;
    email: string;
  };
  likeCount: number;
  isLikedByCurrentUser: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  perPage: number;
  totalPages: number;
}

export interface CreatePostRequest {
  content: string;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

class PostApi {
  private async getHeaders(requireAuth = false): Promise<HeadersInit> {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      const session = await getSession();
      if (session?.accessToken) {
        headers['Authorization'] = `Bearer ${session.accessToken}`;
      }
    }

    return headers;
  }

  // Get feed (public, but auth optional for like status)
  async getFeed(page = 0, size = 20): Promise<PageResponse<Post>> {
    const headers = await this.getHeaders(false);
    const session = await getSession();
    if (session?.accessToken) {
      headers['Authorization'] = `Bearer ${session.accessToken}`;
    }

    const res = await fetch(
      `${API_URL}/api/posts?page=${page}&size=${size}`,
      { headers }
    );

    if (!res.ok) throw new Error('Failed to fetch feed');
    return res.json();
  }

  // Get single post
  async getPost(id: string): Promise<Post> {
    const headers = await this.getHeaders();
    const res = await fetch(`${API_URL}/api/posts/${id}`, { headers });

    if (!res.ok) throw new Error('Failed to fetch post');
    return res.json();
  }

  // Create post (requires auth)
  async createPost(data: CreatePostRequest): Promise<Post> {
    const headers = await this.getHeaders(true);

    const res = await fetch(`${API_URL}/api/posts`, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    });

    if (!res.ok) throw new Error('Failed to create post');
    return res.json();
  }

  // Update post (requires auth)
  async updatePost(id: string, data: CreatePostRequest): Promise<Post> {
    const headers = await this.getHeaders(true);

    const res = await fetch(`${API_URL}/api/posts/${id}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(data),
    });

    if (!res.ok) throw new Error('Failed to update post');
    return res.json();
  }

  // Delete post (requires auth)
  async deletePost(id: string): Promise<void> {
    const headers = await this.getHeaders(true);

    const res = await fetch(`${API_URL}/api/posts/${id}`, {
      method: 'DELETE',
      headers,
    });

    if (!res.ok) throw new Error('Failed to delete post');
  }

  // Toggle like (requires auth)
  async toggleLike(id: string): Promise<LikeResponse> {
    const headers = await this.getHeaders(true);

    const res = await fetch(`${API_URL}/api/posts/${id}/like`, {
      method: 'POST',
      headers,
    });

    if (!res.ok) throw new Error('Failed to toggle like');
    return res.json();
  }

  // Get user's posts
  async getUserPosts(userId: string, page = 0, size = 20): Promise<PageResponse<Post>> {
    const headers = await this.getHeaders();

    const res = await fetch(
      `${API_URL}/api/posts/user/${userId}?page=${page}&size=${size}`,
      { headers }
    );

    if (!res.ok) throw new Error('Failed to fetch user posts');
    return res.json();
  }

  // Get my posts (requires auth)
  async getMyPosts(page = 0, size = 20): Promise<PageResponse<Post>> {
    const headers = await this.getHeaders(true);

    const res = await fetch(
      `${API_URL}/api/posts/me?page=${page}&size=${size}`,
      { headers }
    );

    if (!res.ok) throw new Error('Failed to fetch my posts');
    return res.json();
  }
}

export const postApi = new PostApi();
```

### 12.2 Migration Steps for Frontend

1. **Remove mock data usage** from `app/feed/page.tsx` and `FeedPageClient.tsx`
2. **Update components** to use `postApi` instead of mock functions
3. **Remove** `frontend/lib/mockData.ts` (or keep for tests)
4. **Update** `frontend/app/api/posts/route.ts` to proxy to backend (or remove if calling directly)

---

## 13. Exception Handling

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException ex) {
        log.warn("Post not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("POST_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}

// ErrorResponse.java
public record ErrorResponse(
    String code,
    String message,
    LocalDateTime timestamp
) {
    public ErrorResponse(String code, String message) {
        this(code, message, LocalDateTime.now());
    }
}
```

---

## 14. Configuration

### 14.1 application.yml

```yaml
spring:
  application:
    name: post-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:postdb}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8082

# Auth service client
auth-service:
  url: ${AUTH_SERVICE_URL:http://localhost:8081}

# Logging
logging:
  level:
    com.realestate.post: INFO
    org.springframework.web: INFO

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
```

### 14.2 application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

logging:
  level:
    com.realestate.post: INFO
    org.springframework: WARN

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

---

## 15. Testing Strategy

### 15.1 Test Structure

```
src/test/java/com/realestate/post/
├── controller/
│   ├── PostControllerTest.java         # Unit tests with MockMvc
│   └── PostControllerIntegrationTest.java
├── service/
│   └── PostServiceTest.java            # Unit tests with mocked repos
├── repository/
│   ├── PostRepositoryTest.java         # JPA tests
│   └── LikeRepositoryTest.java
└── integration/
    └── PostFlowIntegrationTest.java    # Full flow tests
```

### 15.2 Key Test Scenarios

| Category | Test Case |
|----------|-----------|
| **Create Post** | Success with valid content |
| | Fail without authentication |
| | Fail with empty content |
| | Fail with content > 5000 chars |
| **Get Feed** | Returns paginated posts |
| | Includes like count |
| | Shows isLikedByCurrentUser correctly |
| **Update Post** | Owner can update |
| | Admin can update any |
| | Non-owner cannot update |
| **Delete Post** | Owner can delete |
| | Admin can delete any |
| | Cascades to delete likes |
| **Like/Unlike** | Toggle creates like |
| | Toggle removes existing like |
| | Prevents duplicate likes |
| | Updates like count correctly |

---

## 16. Deployment

### 16.1 Dockerfile

```dockerfile
# Multi-stage build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 16.2 Helm Chart Structure

```
post-service/helm/post-service/
├── Chart.yaml
├── values.yaml
├── values-local.yaml
├── values-production.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── hpa.yaml
    └── pdb.yaml
```

### 16.3 Docker Compose Addition

```yaml
# Add to docker-compose.yml
post-service:
  build: ./post-service
  ports:
    - "8082:8082"
  environment:
    - DB_HOST=postgres
    - DB_PORT=5432
    - DB_NAME=postdb
    - DB_USER=postgres
    - DB_PASSWORD=postgres
    - AUTH_SERVICE_URL=http://auth-service:8081
  depends_on:
    - postgres
    - auth-service
```

---

## 17. Implementation Plan

### Phase 1: Core Backend (Priority: High)

| Task | Description |
|------|-------------|
| 1.1 | Create post-service Maven project with dependencies |
| 1.2 | Set up database and Flyway migrations |
| 1.3 | Implement Post and Like entities |
| 1.4 | Implement repositories |
| 1.5 | Implement PostService with CRUD operations |
| 1.6 | Implement PostController endpoints |
| 1.7 | Add security filter (UserContext from Kong headers) |
| 1.8 | Write unit tests for service layer |

### Phase 2: Integration (Priority: High)

| Task | Description |
|------|-------------|
| 2.1 | Configure Kong routes for post-service |
| 2.2 | Create Dockerfile and add to docker-compose |
| 2.3 | Write integration tests |
| 2.4 | Test with Kong authentication flow |

### Phase 3: Frontend Migration (Priority: High)

| Task | Description |
|------|-------------|
| 3.1 | Create postApi.ts client |
| 3.2 | Update FeedPageClient to use real API |
| 3.3 | Update PostForm to call postApi.createPost |
| 3.4 | Update LikeButton to call postApi.toggleLike |
| 3.5 | Remove mock data dependencies |
| 3.6 | Test full flow end-to-end |

### Phase 4: Production Readiness (Priority: Medium)

| Task | Description |
|------|-------------|
| 4.1 | Create Helm chart |
| 4.2 | Add Prometheus metrics |
| 4.3 | Configure logging for production |
| 4.4 | Set up HPA for auto-scaling |
| 4.5 | Performance testing |

---

## 18. Success Metrics

| Metric | Target |
|--------|--------|
| API Response Time (p95) | < 200ms |
| API Availability | > 99.5% |
| Test Coverage | > 80% |
| Feed Load Time | < 500ms for 20 posts |
| Like Toggle Latency | < 100ms |

---

## Appendix A: Auth Service Internal API

To support user info fetching, add this endpoint to auth-service:

```java
// In auth-service: InternalUserController.java
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    @GetMapping("/{id}")
    public ResponseEntity<UserInfoResponse> getUserById(@PathVariable UUID id) {
        // Return basic user info (id, name, email)
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, UserInfoResponse>> getUsersByIds(
            @RequestBody List<String> ids) {
        // Return map of userId -> UserInfo for batch fetching
    }
}
```

---

## Appendix B: Sequence Diagrams

### Create Post Flow

```
User          Frontend       Kong          Post-Service      DB
 │               │            │                 │             │
 │──Create Post──▶            │                 │             │
 │               │──POST /api/posts──▶          │             │
 │               │            │──Validate JWT───▶             │
 │               │            │──Add X-User-*───▶             │
 │               │            │                 │──INSERT────▶│
 │               │            │                 │◀────OK──────│
 │               │◀────201 Created──────────────│             │
 │◀──Post Created─│            │                 │             │
```

### Toggle Like Flow

```
User          Frontend       Kong          Post-Service      DB
 │               │            │                 │             │
 │──Click Like───▶            │                 │             │
 │               │──POST /api/posts/{id}/like──▶│             │
 │               │            │──Validate JWT───▶             │
 │               │            │                 │──Check Like─▶│
 │               │            │                 │◀───Result───│
 │               │            │                 │──Toggle─────▶│
 │               │            │                 │◀───OK───────│
 │               │◀────{liked, likeCount}───────│             │
 │◀──Update UI───│            │                 │             │
```

---

**Document Status**: Ready for Review
**Next Steps**: Approval → Implementation Phase 1
