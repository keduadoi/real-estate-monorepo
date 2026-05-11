package com.realestate.comments.service;

import com.realestate.comments.config.CommentProperties;
import com.realestate.comments.dto.CaptchaChallenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaptchaServiceTest {

    private CaptchaService captcha;

    @BeforeEach
    void setup() {
        CommentProperties props = new CommentProperties();
        captcha = new CaptchaService(props);
        captcha.init();
    }

    @Test
    void issuedChallengeHasIdAndQuestion() {
        CaptchaChallenge c = captcha.issue();
        assertNotNull(c.captchaId());
        assertTrue(c.captchaId().startsWith("c-"));
        assertTrue(c.question().matches("\\d \\S \\d"));
    }

    @Test
    void verifyAcceptsCorrectAnswer() {
        CaptchaChallenge c = captcha.issue();
        int answer = computeFromQuestion(c.question());
        assertTrue(captcha.verify(c.captchaId(), String.valueOf(answer)));
    }

    @Test
    void verifyRejectsWrongAnswer() {
        CaptchaChallenge c = captcha.issue();
        int correct = computeFromQuestion(c.question());
        assertFalse(captcha.verify(c.captchaId(), String.valueOf(correct + 1)));
    }

    @Test
    void verifyIsSingleUse() {
        CaptchaChallenge c = captcha.issue();
        int answer = computeFromQuestion(c.question());
        assertTrue(captcha.verify(c.captchaId(), String.valueOf(answer)));
        assertFalse(captcha.verify(c.captchaId(), String.valueOf(answer)),
                "second verify should fail — challenge consumed");
    }

    @Test
    void verifyRejectsUnknownId() {
        assertFalse(captcha.verify("c-no-such", "5"));
        assertFalse(captcha.verify(null, "5"));
        assertFalse(captcha.verify("c-x", null));
    }

    @Test
    void verifyHandlesNonNumericAnswer() {
        CaptchaChallenge c = captcha.issue();
        assertFalse(captcha.verify(c.captchaId(), "abc"));
    }

    private int computeFromQuestion(String q) {
        // q is "a OP b" with single-digit ints
        String[] parts = q.split(" ");
        int a = Integer.parseInt(parts[0]);
        int b = Integer.parseInt(parts[2]);
        return parts[1].equals("+") ? a + b : a * b;
    }
}
