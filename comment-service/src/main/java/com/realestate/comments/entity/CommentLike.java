package com.realestate.comments.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comment_likes")
@IdClass(CommentLike.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {

    @Id
    @Column(name = "comment_id")
    private Long commentId;

    /** user_id when authenticated, ip_hash for anon. */
    @Id
    @Column(length = 64)
    private String identity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class PK implements Serializable {
        private Long commentId;
        private String identity;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(commentId, pk.commentId) && Objects.equals(identity, pk.identity);
        }
        @Override public int hashCode() { return Objects.hash(commentId, identity); }
    }
}
