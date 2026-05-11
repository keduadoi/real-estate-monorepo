package com.realestate.comments.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Set;

/**
 * Lightweight wordlist-based profanity check. Flag-only — never auto-blocks.
 * The result feeds the moderation queue (Comment.flags JSON), so a human can
 * decide whether the comment is actually abusive.
 *
 * Wordlist is intentionally small to keep false-positives low. Vietnamese real
 * estate spam tends to lean heavily on contact-info patterns (Zalo, phone)
 * rather than profanity, so this layer is secondary to rate limits + captcha.
 */
@Service
public class ProfanityFilter {

    private static final Set<String> WORDLIST = Set.of(
            // Light Vietnamese — folded forms
            "dit", "duma", "duconme", "deomeo",
            // English
            "fuck", "shit", "asshole", "bitch", "slut", "whore",
            // Spam / scam markers
            "viagra", "cialis", "casino", "porn", "xxx"
    );

    public boolean isSuspect(String body) {
        if (body == null) return false;
        String folded = Normalizer.normalize(body, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase();
        for (String word : WORDLIST) {
            if (folded.contains(word)) return true;
        }
        return false;
    }
}
