package com.realestate.post.service;

import com.realestate.post.dto.request.CreatePostRequest;
import com.realestate.post.dto.request.CreateReplyRequest;
import com.realestate.post.dto.request.UpdatePostRequest;
import com.realestate.post.dto.response.*;
import com.realestate.post.entity.Like;
import com.realestate.post.entity.Post;
import com.realestate.post.entity.PostImage;
import com.realestate.post.entity.Reply;
import com.realestate.post.exception.ForbiddenException;
import com.realestate.post.exception.InvalidPostException;
import com.realestate.post.exception.PostNotFoundException;
import com.realestate.post.exception.ReplyNotFoundException;
import com.realestate.post.exception.ServiceUnavailableException;
import com.realestate.post.exception.UnauthorizedException;
import com.realestate.post.repository.LikeRepository;
import com.realestate.post.repository.PostImageRepository;
import com.realestate.post.repository.PostRepository;
import com.realestate.post.repository.ReplyRepository;
import com.realestate.post.security.UserContext;
import com.realestate.post.security.UserInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostImageRepository postImageRepository;
    private final ReplyRepository replyRepository;

    /**
     * Create a new post (text, images, or both)
     */
    public PostResponse createPost(CreatePostRequest request) {
        UserInfo currentUser = UserContext.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        boolean hasContent = request.content() != null && !request.content().isBlank();
        boolean hasImages = request.imageUrls() != null && !request.imageUrls().isEmpty();
        if (!hasContent && !hasImages) {
            throw new InvalidPostException("Post must have content or at least one image");
        }

        Post post = Post.builder()
                .content(hasContent ? request.content().trim() : null)
                .userId(currentUser.getId())
                .authorName(currentUser.getFullName())
                .authorEmail(currentUser.getEmail())
                .build();

        if (hasImages) {
            List<String> urls = request.imageUrls();
            for (int i = 0; i < urls.size(); i++) {
                post.getImages().add(PostImage.builder()
                        .post(post)
                        .imageUrl(urls.get(i))
                        .sortOrder(i)
                        .build());
            }
        }

        // saveAndFlush so @CreationTimestamp/@UpdateTimestamp are populated before mapping the response
        Post saved = postRepository.saveAndFlush(post); // Cascades to images
        log.info("Post created: id={}, userId={}, images={}",
                saved.getId(), currentUser.getId(), saved.getImages().size());

        return mapToResponse(saved, currentUser.getId(), 0, 0, false);
    }

    /**
     * Get paginated feed (all posts, newest first)
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "getFeedFallback")
    @RateLimiter(name = "postFeed")
    public PageResponse<PostResponse> getFeed(int page, int size) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PostResponse> responses = enrichPostsWithLikeData(posts.getContent(), currentUserId);

        return new PageResponse<>(
                responses,
                posts.getTotalElements(),
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalPages()
        );
    }

    /**
     * Get single post by ID
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "getPostFallback")
    public PostResponse getPost(UUID postId) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        long likeCount = likeRepository.countByPostId(postId);
        long replyCount = replyRepository.countByPostId(postId);
        boolean isLiked = currentUserId != null && likeRepository.existsByPostIdAndUserId(postId, currentUserId);

        return mapToResponse(post, currentUserId, likeCount, replyCount, isLiked);
    }

    /**
     * Get posts by user ID
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "getPostsByUserFallback")
    public PageResponse<PostResponse> getPostsByUser(String userId, int page, int size) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<PostResponse> responses = enrichPostsWithLikeData(posts.getContent(), currentUserId);

        return new PageResponse<>(
                responses,
                posts.getTotalElements(),
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalPages()
        );
    }

    /**
     * Update post (owner or admin only)
     */
    public PostResponse updatePost(UUID postId, UpdatePostRequest request) {
        String userId = UserContext.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("You can only edit your own posts");
        }

        post.setContent(request.content());
        Post updated = postRepository.saveAndFlush(post);

        log.info("Post updated: id={}, by userId={}", postId, userId);

        long likeCount = likeRepository.countByPostId(postId);
        long replyCount = replyRepository.countByPostId(postId);
        boolean isLiked = likeRepository.existsByPostIdAndUserId(postId, userId);

        return mapToResponse(updated, userId, likeCount, replyCount, isLiked);
    }

    /**
     * Delete post (owner or admin only)
     */
    public void deletePost(UUID postId) {
        String userId = UserContext.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("You can only delete your own posts");
        }

        postRepository.delete(post); // Cascades to likes
        log.info("Post deleted: id={}, by userId={}", postId, userId);
    }

    /**
     * Toggle like on a post (like if not liked, unlike if liked)
     */
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
            Like like = Like.builder()
                    .post(post)
                    .userId(userId)
                    .build();
            likeRepository.save(like);
            isLiked = true;
            log.info("Post liked: postId={}, userId={}", postId, userId);
        }

        long likeCount = likeRepository.countByPostId(postId);
        return new LikeResponse(isLiked, likeCount);
    }

    /**
     * Search posts by content
     */
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "database", fallbackMethod = "searchPostsFallback")
    @RateLimiter(name = "postSearch")
    public PageResponse<PostResponse> searchPosts(String query, int page, int size) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.searchByContent(query, pageable);

        List<PostResponse> responses = enrichPostsWithLikeData(posts.getContent(), currentUserId);

        return new PageResponse<>(
                responses,
                posts.getTotalElements(),
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalPages()
        );
    }

    // Resilience4j fallback methods

    private PageResponse<PostResponse> getFeedFallback(int page, int size, Throwable t) {
        log.error("Database circuit breaker open — getFeed fallback: {}", t.getMessage());
        throw new ServiceUnavailableException("Post feed is temporarily unavailable. Please try again later.");
    }

    private PostResponse getPostFallback(UUID postId, Throwable t) {
        log.error("Database circuit breaker open — getPost fallback for id {}: {}", postId, t.getMessage());
        throw new ServiceUnavailableException("Post service is temporarily unavailable. Please try again later.");
    }

    private PageResponse<PostResponse> getPostsByUserFallback(String userId, int page, int size, Throwable t) {
        log.error("Database circuit breaker open — getPostsByUser fallback for user {}: {}", userId, t.getMessage());
        throw new ServiceUnavailableException("Post service is temporarily unavailable. Please try again later.");
    }

    private PageResponse<PostResponse> searchPostsFallback(String query, int page, int size, Throwable t) {
        log.error("Database circuit breaker open — searchPosts fallback: {}", t.getMessage());
        throw new ServiceUnavailableException("Post search is temporarily unavailable. Please try again later.");
    }

    /**
     * Batch enrich posts with like data for performance
     */
    private List<PostResponse> enrichPostsWithLikeData(List<Post> posts, String currentUserId) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();

        // Batch fetch like counts
        Map<UUID, Long> likeCounts = likeRepository.countLikesByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (UUID) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Batch fetch user's liked posts
        Set<UUID> likedPostIds = currentUserId != null
                ? new HashSet<>(likeRepository.findLikedPostIdsByUserAndPostIds(postIds, currentUserId))
                : Collections.emptySet();

        // Batch fetch reply counts
        Map<UUID, Long> replyCounts = replyRepository.countRepliesByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        arr -> (UUID) arr[0],
                        arr -> (Long) arr[1]
                ));

        // Batch fetch images to avoid N+1 lazy loads
        Map<UUID, List<String>> imagesByPost = postImageRepository
                .findByPostIdInOrderByPostIdAscSortOrderAsc(postIds).stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPost().getId(),
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));

        return posts.stream()
                .map(post -> mapToResponse(
                        post,
                        currentUserId,
                        likeCounts.getOrDefault(post.getId(), 0L),
                        replyCounts.getOrDefault(post.getId(), 0L),
                        likedPostIds.contains(post.getId()),
                        imagesByPost.getOrDefault(post.getId(), Collections.emptyList())
                ))
                .toList();
    }

    /**
     * Get paginated replies for a post (oldest first)
     */
    @Transactional(readOnly = true)
    public PageResponse<ReplyResponse> getReplies(UUID postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Reply> replies = replyRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);

        List<ReplyResponse> responses = replies.getContent().stream()
                .map(this::mapReplyToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                replies.getTotalElements(),
                replies.getNumber(),
                replies.getSize(),
                replies.getTotalPages()
        );
    }

    /**
     * Create a reply on a post (single level — replies cannot be replied to)
     */
    public ReplyResponse createReply(UUID postId, CreateReplyRequest request) {
        UserInfo currentUser = UserContext.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        Reply reply = Reply.builder()
                .post(post)
                .content(request.content().trim())
                .userId(currentUser.getId())
                .authorName(currentUser.getFullName())
                .authorEmail(currentUser.getEmail())
                .build();

        // saveAndFlush so @CreationTimestamp/@UpdateTimestamp are populated before mapping the response
        Reply saved = replyRepository.saveAndFlush(reply);
        log.info("Reply created: id={}, postId={}, userId={}", saved.getId(), postId, currentUser.getId());

        return mapReplyToResponse(saved);
    }

    /**
     * Delete a reply (reply owner or admin only)
     */
    public void deleteReply(UUID postId, UUID replyId) {
        String userId = UserContext.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Reply reply = replyRepository.findById(replyId)
                .filter(r -> r.getPost().getId().equals(postId))
                .orElseThrow(() -> new ReplyNotFoundException(replyId));

        if (!reply.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new ForbiddenException("You can only delete your own replies");
        }

        replyRepository.delete(reply);
        log.info("Reply deleted: id={}, postId={}, by userId={}", replyId, postId, userId);
    }

    /**
     * Map entity to response DTO (images read from the entity, lazy-loaded within transaction)
     */
    private PostResponse mapToResponse(Post post, String currentUserId, long likeCount, long replyCount,
                                       boolean isLiked) {
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .toList();
        return mapToResponse(post, currentUserId, likeCount, replyCount, isLiked, imageUrls);
    }

    /**
     * Map entity to response DTO with pre-fetched image URLs
     */
    private PostResponse mapToResponse(Post post, String currentUserId, long likeCount, long replyCount,
                                       boolean isLiked, List<String> imageUrls) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrls(imageUrls)
                .author(PostAuthorResponse.builder()
                        .id(post.getUserId())
                        .name(post.getAuthorName())
                        .email(post.getAuthorEmail())
                        .build())
                .likeCount(likeCount)
                .replyCount(replyCount)
                .isLikedByCurrentUser(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Map reply entity to response DTO
     */
    private ReplyResponse mapReplyToResponse(Reply reply) {
        return ReplyResponse.builder()
                .id(reply.getId())
                .postId(reply.getPost().getId())
                .content(reply.getContent())
                .author(PostAuthorResponse.builder()
                        .id(reply.getUserId())
                        .name(reply.getAuthorName())
                        .email(reply.getAuthorEmail())
                        .build())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }
}
