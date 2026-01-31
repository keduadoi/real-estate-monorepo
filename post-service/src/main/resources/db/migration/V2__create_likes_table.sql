-- Create likes table
CREATE TABLE likes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id         VARCHAR(36) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate likes
    CONSTRAINT unique_post_user_like UNIQUE (post_id, user_id)
);

-- Index for counting likes per post
CREATE INDEX idx_likes_post_id ON likes(post_id);

-- Index for finding likes by user
CREATE INDEX idx_likes_user_id ON likes(user_id);

-- Add comment
COMMENT ON TABLE likes IS 'Post likes - one like per user per post';
COMMENT ON COLUMN likes.user_id IS 'Reference to user UUID from auth-service';
