package com.realestate.comments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    /** Null for top-level; FK to a top-level comment for a reply (depth ≤ 1). */
    @Column(name = "parent_id")
    private Long parentId;

    /** Null when commenter is anonymous. */
    @Column(name = "user_id", length = 64)
    private String userId;

    /** Required for anon, optional for authenticated (defaults to session display name). */
    @Column(name = "guest_name", length = 50)
    private String guestName;

    @Column(name = "gravatar_hash", length = 32)
    private String gravatarHash;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 200)
    private String userAgent;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "reply_count", nullable = false)
    @Builder.Default
    private Integer replyCount = 0;

    /** Free-form JSON-as-text — e.g. {"profanity":true}. Not queried. */
    @Column(name = "flags")
    private String flags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "hidden_reason", length = 200)
    private String hiddenReason;

    @Column(name = "hidden_by", length = 64)
    private String hiddenBy;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (likeCount == null) likeCount = 0;
        if (replyCount == null) replyCount = 0;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    public boolean isReply() {
        return parentId != null;
    }
}
