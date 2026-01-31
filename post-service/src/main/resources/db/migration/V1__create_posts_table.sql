-- Create posts table
CREATE TABLE posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content         TEXT NOT NULL,
    user_id         VARCHAR(36) NOT NULL,
    author_name     VARCHAR(255),
    author_email    VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fetching posts by user
CREATE INDEX idx_posts_user_id ON posts(user_id);

-- Index for feed ordering (most recent first)
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

-- Add comment
COMMENT ON TABLE posts IS 'User posts for social feed';
COMMENT ON COLUMN posts.user_id IS 'Reference to user UUID from auth-service';
COMMENT ON COLUMN posts.author_name IS 'Denormalized author name for display performance';
COMMENT ON COLUMN posts.author_email IS 'Denormalized author email for display performance';
