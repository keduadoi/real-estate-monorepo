package com.realestate.post.exception;

import java.util.UUID;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(UUID postId) {
        super("Post not found with id: " + postId);
    }

    public PostNotFoundException(String message) {
        super(message);
    }
}
