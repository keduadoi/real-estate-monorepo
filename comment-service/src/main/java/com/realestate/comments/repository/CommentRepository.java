package com.realestate.comments.repository;

import com.realestate.comments.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Top-level (parent_id IS NULL), newest first. Includes hidden so the UI can render stubs. */
    @Query("SELECT c FROM Comment c WHERE c.propertyId = :propertyId AND c.parentId IS NULL " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findTopLevelByProperty(@Param("propertyId") Long propertyId, Pageable pageable);

    /** Children of a parent, oldest first. */
    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParent(@Param("parentId") Long parentId);

    /** Children of multiple parents — used to fetch reply previews in one query. */
    @Query("SELECT c FROM Comment c WHERE c.parentId IN :parentIds ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParents(@Param("parentIds") List<Long> parentIds);

    long countByPropertyIdAndCreatedAtAfter(Long propertyId, LocalDateTime since);

    long countByIpHashAndCreatedAtAfter(String ipHash, LocalDateTime since);

    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + :delta WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    void incrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = GREATEST(c.replyCount - 1, 0) WHERE c.id = :id")
    void decrementReplyCount(@Param("id") Long id);

    /** Reports queue for admin: all comments that have at least one unresolved report. */
    @Query("""
            SELECT c FROM Comment c
            WHERE c.id IN (
                SELECT r.commentId FROM CommentReport r WHERE r.resolvedAt IS NULL
            )
            ORDER BY c.createdAt DESC
            """)
    Page<Comment> findReportedComments(Pageable pageable);

    Page<Comment> findByHiddenAtIsNotNull(Pageable pageable);

    Optional<Comment> findByIdAndPropertyId(Long id, Long propertyId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.hiddenAt IS NOT NULL AND c.hiddenAt < :cutoff")
    int hardDeleteHiddenBefore(@Param("cutoff") LocalDateTime cutoff);
}
