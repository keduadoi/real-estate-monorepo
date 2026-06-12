package com.realestate.post.exception;

import java.util.UUID;

public class ReplyNotFoundException extends RuntimeException {

    public ReplyNotFoundException(UUID replyId) {
        super("Reply not found with id: " + replyId);
    }
}
