-- Create post_replies table (flat, single-level replies on posts)
CREATE TABLE post_replies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    content         TEXT NOT NULL,
    user_id         VARCHAR(36) NOT NULL,
    author_name     VARCHAR(255),
    author_email    VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fetching replies by post (oldest first within a post)
CREATE INDEX idx_post_replies_post_id ON post_replies(post_id);
CREATE INDEX idx_post_replies_user_id ON post_replies(user_id);

-- Add comments
COMMENT ON TABLE post_replies IS 'Single-level replies on social feed posts (no nested replies)';
COMMENT ON COLUMN post_replies.user_id IS 'Reference to user UUID from auth-service';
COMMENT ON COLUMN post_replies.author_name IS 'Denormalized author name for display performance';
COMMENT ON COLUMN post_replies.author_email IS 'Denormalized author email for display performance';
