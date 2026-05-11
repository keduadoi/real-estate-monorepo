package com.realestate.comments.service;

import com.realestate.comments.config.CommentProperties;
import com.realestate.comments.exception.RateLimitedException;
import com.realestate.comments.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * DB-backed sliding-window rate limit. Counts rows in the last N minutes and
 * compares against thresholds in {@link CommentProperties.RateLimit}.
 *
 * Cheap because the indexed columns (ip_hash, user_id, property_id) are all
 * already there for normal queries; the sliding window is a single COUNT(*).
 * We could move to Caffeine counters for higher throughput later — keep this
 * simple & correct for v1.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final CommentProperties props;
    private final CommentRepository commentRepo;

    /** Throws if any of the per-IP / per-user / per-property buckets is over budget. */
    public void enforceWriteLimits(String ipHash, String userId, Long propertyId) {
        LocalDateTime now = LocalDateTime.now();
        var rl = props.getRateLimit();

        if (propertyId != null) {
            long inLastMin = commentRepo.countByPropertyIdAndCreatedAtAfter(
                    propertyId, now.minusMinutes(1));
            if (inLastMin >= rl.getPerPropertyPerMin()) {
                throw new RateLimitedException(60, "property write rate exceeded");
            }
        }

        if (userId != null) {
            long in5min = commentRepo.countByUserIdAndCreatedAtAfter(userId, now.minusMinutes(5));
            if (in5min >= rl.getUserPerIdPer5min()) {
                throw new RateLimitedException(300, "user write rate exceeded (5 min)");
            }
            long inDay = commentRepo.countByUserIdAndCreatedAtAfter(userId, now.minusDays(1));
            if (inDay >= rl.getUserPerIdPerDay()) {
                throw new RateLimitedException(86400, "user write rate exceeded (daily)");
            }
            return; // logged-in users skip the per-IP bucket
        }

        // Anon path
        if (ipHash != null) {
            long in5min = commentRepo.countByIpHashAndCreatedAtAfter(ipHash, now.minusMinutes(5));
            if (in5min >= rl.getAnonPerIpPer5min()) {
                throw new RateLimitedException(300, "anon write rate exceeded (5 min)");
            }
            long inDay = commentRepo.countByIpHashAndCreatedAtAfter(ipHash, now.minusDays(1));
            if (inDay >= rl.getAnonPerIpPerDay()) {
                throw new RateLimitedException(86400, "anon write rate exceeded (daily)");
            }
        }
    }
}
