package com.realestate.comments.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    void cleanCommentsAreOk() {
        assertFalse(filter.isSuspect("Còn không bạn? Mình quan tâm."));
        assertFalse(filter.isSuspect("Looks like a great property!"));
        assertFalse(filter.isSuspect(null));
        assertFalse(filter.isSuspect(""));
    }

    @Test
    void englishProfanityIsFlagged() {
        assertTrue(filter.isSuspect("this is fucking great"));
        assertTrue(filter.isSuspect("what an asshole owner"));
    }

    @Test
    void scamMarkersAreFlagged() {
        assertTrue(filter.isSuspect("buy viagra cheap"));
        assertTrue(filter.isSuspect("CASINO promo code"));
    }

    @Test
    void vietnameseDiacriticsAreFolded() {
        // Confirms the fold step: an accented "đ" + tones still match the
        // ASCII "duma" entry in the wordlist when the source is contiguous.
        assertTrue(filter.isSuspect("ĐụMá cái này"));
    }

    @Test
    void wordsSeparatedByWhitespaceAreNotMatched() {
        // Documents a known limitation: matching is substring-only, so
        // "đụ má" with a space won't trigger. Acceptable for v1 — false
        // positives matter more than recall here.
        assertFalse(filter.isSuspect("đụ má cái nhà"));
    }
}
