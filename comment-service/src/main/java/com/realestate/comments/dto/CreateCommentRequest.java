package com.realestate.comments.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Size(max = 1000) String body,
        @Size(max = 50) String displayName,
        @Pattern(regexp = "^[a-f0-9]{32}$|^$") String gravatarHash,
        String captchaId,
        String captchaAnswer,
        /** Honeypot — must be empty/null. Anything filled in here = bot, drop silently. */
        String website
) {
    /** Defensive: convert blank strings to null for consistent handling downstream. */
    public CreateCommentRequest sanitized() {
        return new CreateCommentRequest(
                body == null ? null : body,
                blankToNull(displayName),
                blankToNull(gravatarHash),
                blankToNull(captchaId),
                blankToNull(captchaAnswer),
                website
        );
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
