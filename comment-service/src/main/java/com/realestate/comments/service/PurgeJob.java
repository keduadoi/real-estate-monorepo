package com.realestate.comments.service;

import com.realestate.comments.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Hard-purges hidden comments older than 30 days. Runs once a day at 03:00 local time.
 * Soft-deleted rows stay queryable by admins for the retention window so a
 * mistakenly-hidden comment can be restored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurgeJob {

    private static final int RETENTION_DAYS = 30;

    private final CommentRepository commentRepo;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purge() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int n = commentRepo.hardDeleteHiddenBefore(cutoff);
        if (n > 0) log.info("Purged {} hidden comments older than {} days", n, RETENTION_DAYS);
    }
}
