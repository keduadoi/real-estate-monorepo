package com.realestate.comments.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.realestate.comments.config.CommentProperties;
import com.realestate.comments.dto.CaptchaChallenge;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * In-memory single-use math captcha. Generates `a + b` or `a × b` with 1 ≤ a,b ≤ 9.
 * Stores the answer keyed by a UUID (captchaId) for `comments.captcha.ttl-seconds`.
 * On verify, the entry is removed regardless of correctness — no replay.
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final SecureRandom RNG = new SecureRandom();

    private final CommentProperties props;
    private Cache<String, Integer> challenges;

    @PostConstruct
    void init() {
        challenges = Caffeine.newBuilder()
                .maximumSize(props.getCaptcha().getMaxPending())
                .expireAfterWrite(Duration.ofSeconds(props.getCaptcha().getTtlSeconds()))
                .build();
    }

    public CaptchaChallenge issue() {
        int a = 1 + RNG.nextInt(9);
        int b = 1 + RNG.nextInt(9);
        boolean useMul = RNG.nextBoolean();
        int answer = useMul ? a * b : a + b;
        char op = useMul ? '×' : '+';
        String id = "c-" + UUID.randomUUID();
        challenges.put(id, answer);
        return new CaptchaChallenge(id, a + " " + op + " " + b);
    }

    /**
     * Verify and consume the challenge. Returns true only when the id is known
     * AND the answer parses as the expected integer.
     */
    public boolean verify(String captchaId, String answer) {
        if (captchaId == null || answer == null) return false;
        Integer expected = challenges.asMap().remove(captchaId);
        if (expected == null) return false;
        try {
            return Integer.parseInt(answer.trim()) == expected;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
