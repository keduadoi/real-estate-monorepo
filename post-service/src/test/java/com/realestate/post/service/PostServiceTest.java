package com.realestate.post.service;

import com.realestate.post.dto.request.CreatePostRequest;
import com.realestate.post.dto.request.UpdatePostRequest;
import com.realestate.post.dto.response.LikeResponse;
import com.realestate.post.dto.response.PageResponse;
import com.realestate.post.dto.response.PostResponse;
import com.realestate.post.entity.Like;
import com.realestate.post.entity.Post;
import com.realestate.post.exception.ForbiddenException;
import com.realestate.post.exception.PostNotFoundException;
import com.realestate.post.exception.UnauthorizedException;
import com.realestate.post.repository.LikeRepository;
import com.realestate.post.repository.PostRepository;
import com.realestate.post.security.UserContext;
import com.realestate.post.security.UserInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private PostService postService;

    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-456";
    private static final UUID POST_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UserInfo user = UserInfo.builder()
                .id(USER_ID)
                .email("test@example.com")
                .name("Test User")
                .rolesString("ROLE_USER")
                .build();
        UserContext.setCurrentUser(user);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createPost_Success() {
        CreatePostRequest request = new CreatePostRequest("Hello World!");
        Post savedPost = createPost(POST_ID, "Hello World!", USER_ID);

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        PostResponse response = postService.createPost(request);

        assertThat(response.id()).isEqualTo(POST_ID);
        assertThat(response.content()).isEqualTo("Hello World!");
        assertThat(response.author().id()).isEqualTo(USER_ID);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createPost_Unauthorized() {
        UserContext.clear();
        CreatePostRequest request = new CreatePostRequest("Hello World!");

        assertThatThrownBy(() -> postService.createPost(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getFeed_Success() {
        Post post1 = createPost(UUID.randomUUID(), "Post 1", USER_ID);
        Post post2 = createPost(UUID.randomUUID(), "Post 2", OTHER_USER_ID);
        List<Post> posts = List.of(post1, post2);

        when(postRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(posts, PageRequest.of(0, 20), 2));
        when(likeRepository.countLikesByPostIds(any())).thenReturn(Collections.emptyList());
        when(likeRepository.findLikedPostIdsByUserAndPostIds(any(), eq(USER_ID)))
                .thenReturn(Collections.emptyList());

        PageResponse<PostResponse> response = postService.getFeed(0, 20);

        assertThat(response.data()).hasSize(2);
        assertThat(response.total()).isEqualTo(2);
    }

    @Test
    void getPost_Success() {
        Post post = createPost(POST_ID, "Test content", USER_ID);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(likeRepository.countByPostId(POST_ID)).thenReturn(5L);
        when(likeRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(true);

        PostResponse response = postService.getPost(POST_ID);

        assertThat(response.id()).isEqualTo(POST_ID);
        assertThat(response.likeCount()).isEqualTo(5);
        assertThat(response.isLikedByCurrentUser()).isTrue();
    }

    @Test
    void getPost_NotFound() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(POST_ID))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void updatePost_OwnerSuccess() {
        Post post = createPost(POST_ID, "Original", USER_ID);
        Post updatedPost = createPost(POST_ID, "Updated", USER_ID);
        UpdatePostRequest request = new UpdatePostRequest("Updated");

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(updatedPost);
        when(likeRepository.countByPostId(POST_ID)).thenReturn(0L);
        when(likeRepository.existsByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(false);

        PostResponse response = postService.updatePost(POST_ID, request);

        assertThat(response.content()).isEqualTo("Updated");
    }

    @Test
    void updatePost_NotOwner_Forbidden() {
        Post post = createPost(POST_ID, "Original", OTHER_USER_ID);
        UpdatePostRequest request = new UpdatePostRequest("Updated");

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(POST_ID, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deletePost_OwnerSuccess() {
        Post post = createPost(POST_ID, "To delete", USER_ID);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        postService.deletePost(POST_ID);

        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_NotOwner_Forbidden() {
        Post post = createPost(POST_ID, "To delete", OTHER_USER_ID);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(POST_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void toggleLike_AddLike() {
        Post post = createPost(POST_ID, "Content", OTHER_USER_ID);

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(Optional.empty());
        when(likeRepository.save(any(Like.class))).thenReturn(new Like());
        when(likeRepository.countByPostId(POST_ID)).thenReturn(1L);

        LikeResponse response = postService.toggleLike(POST_ID);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    void toggleLike_RemoveLike() {
        Post post = createPost(POST_ID, "Content", OTHER_USER_ID);
        Like existingLike = Like.builder()
                .id(UUID.randomUUID())
                .post(post)
                .userId(USER_ID)
                .build();

        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId(POST_ID, USER_ID)).thenReturn(Optional.of(existingLike));
        when(likeRepository.countByPostId(POST_ID)).thenReturn(0L);

        LikeResponse response = postService.toggleLike(POST_ID);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0);
        verify(likeRepository).delete(existingLike);
    }

    private Post createPost(UUID id, String content, String userId) {
        return Post.builder()
                .id(id)
                .content(content)
                .userId(userId)
                .authorName("Test User")
                .authorEmail("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
