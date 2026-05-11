package com.realestate.comments.service;

import com.realestate.comments.config.CommentProperties;
import com.realestate.comments.dto.*;
import com.realestate.comments.entity.Comment;
import com.realestate.comments.entity.CommentLike;
import com.realestate.comments.entity.CommentReport;
import com.realestate.comments.exception.CommentNotFoundException;
import com.realestate.comments.exception.ForbiddenException;
import com.realestate.comments.exception.PropertyNotFoundException;
import com.realestate.comments.exception.ValidationException;
import com.realestate.comments.repository.CommentLikeRepository;
import com.realestate.comments.repository.CommentReportRepository;
import com.realestate.comments.repository.CommentRepository;
import com.realestate.comments.security.UserContext;
import com.realestate.comments.upstream.PropertyExistenceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepo;
    private final CommentLikeRepository likeRepo;
    private final CommentReportRepository reportRepo;
    private final CaptchaService captcha;
    private final ProfanityFilter profanityFilter;
    private final RateLimitService rateLimit;
    private final PropertyExistenceClient propertyClient;
    private final CommentProperties props;

    // ---- Read paths ----------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> listForProperty(Long propertyId,
                                                          int page, int perPage,
                                                          UserContext viewer) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(perPage, 1), 50));
        Page<Comment> roots = commentRepo.findTopLevelByProperty(propertyId, pageable);

        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParent = rootIds.isEmpty()
                ? Map.of()
                : commentRepo.findRepliesByParents(rootIds).stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // Determine ownership of the property (one cached upstream lookup).
        String ownerUserId = propertyClient.lookup(propertyId).ownerUserId();

        // Likes-by-current viewer in one query for both root + reply ids.
        String viewerIdentity = viewer.getUserId();
        Set<Long> likedByCurrent;
        if (viewerIdentity != null) {
            List<Long> allIds = new ArrayList<>(rootIds);
            repliesByParent.values().forEach(list -> list.forEach(c -> allIds.add(c.getId())));
            likedByCurrent = allIds.isEmpty() ? Set.of()
                    : new HashSet<>(likeRepo.findLikedCommentIds(viewerIdentity, allIds));
        } else {
            likedByCurrent = Set.of();
        }

        List<CommentResponse> data = roots.getContent().stream()
                .map(c -> toResponse(c,
                        repliesByParent.getOrDefault(c.getId(), List.of()),
                        ownerUserId, viewer, likedByCurrent))
                .toList();

        return new PagedResponse<>(
                data,
                roots.getTotalElements(),
                roots.getNumber(),
                roots.getSize(),
                roots.getTotalPages());
    }

    // ---- Write paths ---------------------------------------------------------

    @Transactional
    public CommentResponse create(Long propertyId,
                                  CreateCommentRequest request,
                                  UserContext viewer,
                                  String ipHash,
                                  String userAgent) {
        if (props.isDisabled()) {
            throw new ValidationException("COMMENTS_DISABLED", "Comments are disabled");
        }

        // Honeypot — be silent. Returning 200 with id=-1 wastes the bot's time.
        if (request.website() != null && !request.website().isBlank()) {
            log.info("honeypot triggered, dropping comment silently");
            return honeypotResponse(propertyId);
        }

        validateBody(request.body());
        validateAndPickName(request, viewer);

        // Anonymous writes need a captcha.
        if (!viewer.isAuthenticated()) {
            if (!captcha.verify(request.captchaId(), request.captchaAnswer())) {
                throw new ValidationException("CAPTCHA_FAILED",
                        "Captcha incorrect or expired");
            }
        }

        var meta = propertyClient.lookup(propertyId);
        if (!meta.exists()) {
            throw new PropertyNotFoundException(propertyId);
        }

        rateLimit.enforceWriteLimits(ipHash, viewer.getUserId(), propertyId);

        boolean profanity = profanityFilter.isSuspect(request.body());

        Comment c = Comment.builder()
                .propertyId(propertyId)
                .parentId(null)
                .userId(viewer.getUserId())
                .guestName(viewer.isAuthenticated() ? viewer.getName() : request.displayName())
                .gravatarHash(viewer.isAuthenticated() ? null : request.gravatarHash())
                .body(normalize(request.body()))
                .ipHash(ipHash)
                .userAgent(truncate(userAgent, 200))
                .flags(profanity ? "{\"profanity\":true}" : null)
                .build();
        commentRepo.save(c);

        return toResponse(c, List.of(), meta.ownerUserId(), viewer, Set.of());
    }

    @Transactional
    public CommentResponse reply(Long parentId,
                                 CreateCommentRequest request,
                                 UserContext viewer,
                                 String ipHash,
                                 String userAgent) {
        Comment parent = commentRepo.findById(parentId)
                .orElseThrow(() -> new CommentNotFoundException(parentId));
        if (parent.isReply()) {
            throw new ValidationException("MAX_DEPTH_EXCEEDED",
                    "Replies are limited to one level");
        }

        // Reuse the create() pre-checks — captcha, rate-limit, profanity, etc.
        if (props.isDisabled()) {
            throw new ValidationException("COMMENTS_DISABLED", "Comments are disabled");
        }
        if (request.website() != null && !request.website().isBlank()) {
            return honeypotResponse(parent.getPropertyId());
        }
        validateBody(request.body());
        validateAndPickName(request, viewer);
        if (!viewer.isAuthenticated()
                && !captcha.verify(request.captchaId(), request.captchaAnswer())) {
            throw new ValidationException("CAPTCHA_FAILED",
                    "Captcha incorrect or expired");
        }
        rateLimit.enforceWriteLimits(ipHash, viewer.getUserId(), parent.getPropertyId());

        boolean profanity = profanityFilter.isSuspect(request.body());

        Comment c = Comment.builder()
                .propertyId(parent.getPropertyId())
                .parentId(parent.getId())
                .userId(viewer.getUserId())
                .guestName(viewer.isAuthenticated() ? viewer.getName() : request.displayName())
                .gravatarHash(viewer.isAuthenticated() ? null : request.gravatarHash())
                .body(normalize(request.body()))
                .ipHash(ipHash)
                .userAgent(truncate(userAgent, 200))
                .flags(profanity ? "{\"profanity\":true}" : null)
                .build();
        commentRepo.save(c);
        commentRepo.incrementReplyCount(parent.getId());

        String ownerUserId = propertyClient.lookup(parent.getPropertyId()).ownerUserId();
        return toResponse(c, List.of(), ownerUserId, viewer, Set.of());
    }

    @Transactional
    public CommentResponse update(Long commentId, UpdateCommentRequest request, UserContext viewer) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!viewer.isAuthenticated() || !viewer.getUserId().equals(c.getUserId())) {
            throw new ForbiddenException("Only the author can edit this comment");
        }
        if (c.isHidden()) {
            throw new ForbiddenException("Hidden comments cannot be edited");
        }
        Duration window = Duration.ofMinutes(props.getEditWindowMinutes());
        if (Duration.between(c.getCreatedAt(), LocalDateTime.now()).compareTo(window) > 0) {
            throw new ValidationException("EDIT_WINDOW_EXPIRED",
                    "Edit window of " + props.getEditWindowMinutes() + " minutes has passed");
        }

        validateBody(request.body());
        c.setBody(normalize(request.body()));
        c.setUpdatedAt(LocalDateTime.now());
        commentRepo.save(c);

        String ownerUserId = propertyClient.lookup(c.getPropertyId()).ownerUserId();
        return toResponse(c, replies(c.getId()), ownerUserId, viewer, Set.of());
    }

    @Transactional
    public void delete(Long commentId, UserContext viewer) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        boolean isAuthor = viewer.isAuthenticated() && viewer.getUserId().equals(c.getUserId());
        if (!isAuthor && !viewer.isAdmin()) {
            throw new ForbiddenException("Not allowed to delete this comment");
        }
        // Soft-delete only — we keep the row 30 days for audit/recovery.
        c.setHiddenAt(LocalDateTime.now());
        c.setHiddenReason(isAuthor ? "deleted by author" : "deleted by admin");
        c.setHiddenBy(viewer.getUserId());
        commentRepo.save(c);

        if (c.isReply()) {
            commentRepo.decrementReplyCount(c.getParentId());
        }
    }

    @Transactional
    public boolean toggleLike(Long commentId, UserContext viewer, String ipHash) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        String identity = viewer.isAuthenticated() ? viewer.getUserId() : ipHash;
        if (identity == null) {
            throw new ValidationException("BAD_REQUEST", "No identity for like");
        }
        if (likeRepo.existsByCommentIdAndIdentity(commentId, identity)) {
            likeRepo.deleteByCommentIdAndIdentity(commentId, identity);
            commentRepo.incrementLikeCount(c.getId(), -1);
            return false;
        }
        likeRepo.save(CommentLike.builder()
                .commentId(commentId)
                .identity(identity)
                .build());
        commentRepo.incrementLikeCount(c.getId(), 1);
        return true;
    }

    @Transactional
    public void report(Long commentId, ReportCommentRequest request, UserContext viewer, String ipHash) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        String reporter = viewer.isAuthenticated() ? viewer.getUserId() : ipHash;
        if (reporter == null) {
            throw new ValidationException("BAD_REQUEST", "No identity for report");
        }
        if (reportRepo.existsByCommentIdAndReporter(c.getId(), reporter)) {
            return; // idempotent — silent success
        }
        reportRepo.save(CommentReport.builder()
                .commentId(c.getId())
                .reporter(reporter)
                .reason(request.reason())
                .note(request.note())
                .build());
    }

    // ---- Admin paths ---------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<CommentResponse> adminListReported(int page, int perPage, UserContext viewer) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(perPage, 1), 50));
        Page<Comment> rows = commentRepo.findReportedComments(pageable);
        List<CommentResponse> data = rows.getContent().stream()
                .map(c -> toResponse(c, List.of(),
                        propertyClient.lookup(c.getPropertyId()).ownerUserId(),
                        viewer, Set.of()))
                .toList();
        return new PagedResponse<>(data,
                rows.getTotalElements(), rows.getNumber(), rows.getSize(), rows.getTotalPages());
    }

    @Transactional
    public void hide(Long commentId, String reason, UserContext admin) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        c.setHiddenAt(LocalDateTime.now());
        c.setHiddenReason(reason);
        c.setHiddenBy(admin.getUserId());
        commentRepo.save(c);
    }

    @Transactional
    public void restore(Long commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        c.setHiddenAt(null);
        c.setHiddenReason(null);
        c.setHiddenBy(null);
        commentRepo.save(c);
    }

    // ---- Validation helpers --------------------------------------------------

    private void validateBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new ValidationException("BODY_EMPTY", "Body cannot be empty");
        }
        if (body.length() > props.getBodyMaxLength()) {
            throw new ValidationException("BODY_TOO_LONG",
                    "Body cannot exceed " + props.getBodyMaxLength() + " chars");
        }
    }

    private void validateAndPickName(CreateCommentRequest request, UserContext viewer) {
        if (viewer.isAuthenticated()) return;
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new ValidationException("DISPLAY_NAME_REQUIRED",
                    "Display name is required for anonymous comments");
        }
        if (request.displayName().length() > props.getDisplayNameMaxLength()) {
            throw new ValidationException("DISPLAY_NAME_TOO_LONG",
                    "Display name too long (max " + props.getDisplayNameMaxLength() + ")");
        }
        // Cheap anti-impersonation: no embedded URLs in name
        if (request.displayName().toLowerCase().matches(".*(http|www\\.).*")) {
            throw new ValidationException("INVALID_DISPLAY_NAME",
                    "Display name cannot contain URLs");
        }
    }

    private static String normalize(String body) {
        return body.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private List<Comment> replies(Long parentId) {
        return commentRepo.findRepliesByParent(parentId);
    }

    /** Bot-friendly empty success: same shape, fake id. Never inserts a row. */
    private CommentResponse honeypotResponse(Long propertyId) {
        return new CommentResponse(
                -1L, propertyId, null, null, "spam", null,
                false, false, "", 0, false, 0, List.of(),
                LocalDateTime.now(), null, false, false);
    }

    // ---- Mapping -------------------------------------------------------------

    private CommentResponse toResponse(Comment c,
                                       List<Comment> children,
                                       String propertyOwnerUserId,
                                       UserContext viewer,
                                       Set<Long> likedByCurrent) {
        boolean isOwner = propertyOwnerUserId != null
                && c.getUserId() != null
                && c.getUserId().equals(propertyOwnerUserId);

        // Hidden rows get a stub body + null identity.
        boolean hidden = c.isHidden();
        String body = hidden ? null : c.getBody();
        String displayName = hidden ? null : pickDisplayName(c);
        String gravatar = hidden ? null : c.getGravatarHash();

        boolean editable = !hidden
                && viewer.isAuthenticated()
                && viewer.getUserId().equals(c.getUserId())
                && Duration.between(c.getCreatedAt(), LocalDateTime.now()).compareTo(
                        Duration.ofMinutes(props.getEditWindowMinutes())) < 0;

        List<CommentResponse> replyDtos = children.stream()
                .map(child -> toResponse(child, List.of(), propertyOwnerUserId, viewer, likedByCurrent))
                .toList();

        return new CommentResponse(
                c.getId(),
                c.getPropertyId(),
                c.getParentId(),
                hidden ? null : c.getUserId(),
                displayName,
                gravatar,
                isOwner,
                false,                           // isAdmin badge — could later check role; skip for v1
                body,
                c.getLikeCount(),
                likedByCurrent.contains(c.getId()),
                c.getReplyCount(),
                replyDtos,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                editable,
                hidden);
    }

    private static String pickDisplayName(Comment c) {
        if (c.getGuestName() != null && !c.getGuestName().isBlank()) return c.getGuestName();
        return c.getUserId() == null ? "Anonymous" : "User";
    }
}
