package com.realestate.post.repository;

import com.realestate.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    /**
     * Find all posts ordered by creation date (newest first)
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find posts by specific user ordered by creation date
     */
    Page<Post> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Count posts by user
     */
    long countByUserId(String userId);

    /**
     * Search posts by content (case-insensitive)
     */
    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Page<Post> searchByContent(@Param("query") String query, Pageable pageable);

    /**
     * Check if post exists and belongs to user
     */
    boolean existsByIdAndUserId(UUID id, String userId);
}
