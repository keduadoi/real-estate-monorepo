package com.realestate.comments.service;

import com.realestate.comments.config.CommentProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

/**
 * HMAC-SHA256 hash of the client IP, hex-encoded. We never store the raw IP;
 * the hashed form is enough for rate-limit lookups + abuse review.
 *
 * The HMAC key comes from `comments.ip-hash-secret` (env: IP_HASH_SECRET) and
 * should rotate yearly to invalidate old hashes.
 */
@Service
@RequiredArgsConstructor
public class IpHasher {

    private static final String ALG = "HmacSHA256";

    private final CommentProperties props;

    public String hash(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance(ALG);
            mac.init(new SecretKeySpec(
                    props.getIpHashSecret().getBytes(StandardCharsets.UTF_8), ALG));
            byte[] out = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute IP hash", e);
        }
    }

    /** Extract the most-likely client IP, preferring X-Forwarded-For from Kong. */
    public String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma < 0 ? xff : xff.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }
}
