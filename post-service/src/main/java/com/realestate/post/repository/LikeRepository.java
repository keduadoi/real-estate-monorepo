package com.realestate.post.repository;

import com.realestate.post.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {

    /**
     * Check if user has liked a post
     */
    boolean existsByPostIdAndUserId(UUID postId, String userId);

    /**
     * Find like by post and user
     */
    Optional<Like> findByPostIdAndUserId(UUID postId, String userId);

    /**
     * Count likes for a post
     */
    long countByPostId(UUID postId);

    /**
     * Get all likes for a post
     */
    List<Like> findByPostId(UUID postId);

    /**
     * Get all likes by a user
     */
    List<Like> findByUserId(String userId);

    /**
     * Delete like by post and user
     */
    void deleteByPostIdAndUserId(UUID postId, String userId);

    /**
     * Batch count likes for multiple posts (performance optimization for feed)
     * Returns list of [postId, count] pairs
     */
    @Query("SELECT l.post.id, COUNT(l) FROM Like l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countLikesByPostIds(@Param("postIds") List<UUID> postIds);

    /**
     * Check which posts the user has liked from a list (for feed)
     * Returns list of post IDs that the user has liked
     */
    @Query("SELECT l.post.id FROM Like l WHERE l.post.id IN :postIds AND l.userId = :userId")
    List<UUID> findLikedPostIdsByUserAndPostIds(@Param("postIds") List<UUID> postIds, @Param("userId") String userId);
}
