package com.realestate.post.repository;

import com.realestate.post.entity.Reply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, UUID> {

    /**
     * Find replies for a post, oldest first (conversation order)
     */
    Page<Reply> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    /**
     * Count replies for a post
     */
    long countByPostId(UUID postId);

    /**
     * Batch count replies for multiple posts (feed enrichment)
     */
    @Query("SELECT r.post.id, COUNT(r) FROM Reply r WHERE r.post.id IN :postIds GROUP BY r.post.id")
    List<Object[]> countRepliesByPostIds(@Param("postIds") List<UUID> postIds);
}
