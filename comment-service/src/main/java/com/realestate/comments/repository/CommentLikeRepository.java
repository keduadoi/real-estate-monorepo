package com.realestate.comments.repository;

import com.realestate.comments.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLike.PK> {

    boolean existsByCommentIdAndIdentity(Long commentId, String identity);

    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.commentId = :commentId AND l.identity = :identity")
    int deleteByCommentIdAndIdentity(@Param("commentId") Long commentId,
                                     @Param("identity") String identity);

    @Query("SELECT l.commentId FROM CommentLike l " +
            "WHERE l.identity = :identity AND l.commentId IN :commentIds")
    List<Long> findLikedCommentIds(@Param("identity") String identity,
                                   @Param("commentIds") Collection<Long> commentIds);

    long countByIdentityAndCreatedAtAfter(String identity, LocalDateTime since);
}
