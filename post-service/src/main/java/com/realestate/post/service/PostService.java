package com.realestate.post.service;

import com.realestate.post.dto.request.CreatePostRequest;
import com.realestate.post.dto.request.UpdatePostRequest;
import com.realestate.post.dto.response.*;
import com.realestate.post.entity.Like;
import com.realestate.post.entity.Post;
import com.realestate.post.exception.ForbiddenException;
import com.realestate.post.exception.PostNotFoundException;
import com.realestate.post.exception.UnauthorizedException;
import com.realestate.post.repository.LikeRepository;
import com.realestate.post.repository.PostRepository;
import com.realestate.post.security.UserContext;
import com.realestate.post.security.UserInfo;
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

    /**
     * Create a new post
     */
    public PostResponse createPost(CreatePostRequest request) {
        UserInfo currentUser = UserContext.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        Post post = Post.builder()
                .content(request.content())
                .userId(currentUser.getId())
                .authorName(currentUser.getFullName())
                .authorEmail(currentUser.getEmail())
                .build();

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, userId={}", saved.getId(), currentUser.getId());

        return mapToResponse(saved, currentUser.getId(), 0, false);
    }

    /**
     * Get paginated feed (all posts, newest first)
     */
    @Transactional(readOnly = true)
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
    public PostResponse getPost(UUID postId) {
        String currentUserId = UserContext.getCurrentUserId().orElse(null);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        long likeCount = likeRepository.countByPostId(postId);
        boolean isLiked = currentUserId != null && likeRepository.existsByPostIdAndUserId(postId, currentUserId);

        return mapToResponse(post, currentUserId, likeCount, isLiked);
    }

    /**
     * Get posts by user ID
     */
    @Transactional(readOnly = true)
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
        Post updated = postRepository.save(post);

        log.info("Post updated: id={}, by userId={}", postId, userId);

        long likeCount = likeRepository.countByPostId(postId);
        boolean isLiked = likeRepository.existsByPostIdAndUserId(postId, userId);

        return mapToResponse(updated, userId, likeCount, isLiked);
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

        return posts.stream()
                .map(post -> mapToResponse(
                        post,
                        currentUserId,
                        likeCounts.getOrDefault(post.getId(), 0L),
                        likedPostIds.contains(post.getId())
                ))
                .toList();
    }

    /**
     * Map entity to response DTO
     */
    private PostResponse mapToResponse(Post post, String currentUserId, long likeCount, boolean isLiked) {
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .author(PostAuthorResponse.builder()
                        .id(post.getUserId())
                        .name(post.getAuthorName())
                        .email(post.getAuthorEmail())
                        .build())
                .likeCount(likeCount)
                .isLikedByCurrentUser(isLiked)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
