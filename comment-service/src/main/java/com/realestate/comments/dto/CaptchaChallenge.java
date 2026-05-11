package com.realestate.comments.dto;

public record CaptchaChallenge(
        String captchaId,
        String question
) {
}
