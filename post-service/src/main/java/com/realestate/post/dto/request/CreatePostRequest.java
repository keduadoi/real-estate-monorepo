package com.realestate.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
    // Optional: a post must have content or at least one image (enforced in PostService)
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    String content,

    @Size(max = 10, message = "A post can have at most 10 images")
    List<@NotBlank(message = "Image URL must not be blank")
         @Size(max = 1024, message = "Image URL must be at most 1024 characters") String> imageUrls
) {}
