package com.realestate.post.repository;

import com.realestate.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, UUID> {

    /**
     * Batch fetch images for multiple posts (feed enrichment), ordered for display
     */
    List<PostImage> findByPostIdInOrderByPostIdAscSortOrderAsc(List<UUID> postIds);
}
