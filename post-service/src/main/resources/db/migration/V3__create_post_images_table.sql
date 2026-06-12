-- Create post_images table for image attachments
CREATE TABLE post_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    image_url       VARCHAR(1024) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fetching images by post
CREATE INDEX idx_post_images_post_id ON post_images(post_id);

-- Add comments
COMMENT ON TABLE post_images IS 'Image attachments for social feed posts';
COMMENT ON COLUMN post_images.sort_order IS 'Display order of images within a post (0-based)';
