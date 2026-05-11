package com.realestate.comments.exception;

import lombok.Getter;

@Getter
public class RateLimitedException extends RuntimeException {

    /** Hint to the client; surfaced as Retry-After header. */
    private final int retryAfterSeconds;

    public RateLimitedException(int retryAfterSeconds, String message) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
