-- =============================================================================
-- V1__create_comment_tables.sql
-- Property comments + likes + reports.  See docs/PROPERTY_COMMENTS_PRD.md.
-- =============================================================================

CREATE TABLE comments (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    parent_id       BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    user_id         VARCHAR(64),
    guest_name      VARCHAR(50),
    gravatar_hash   VARCHAR(32),
    body            TEXT NOT NULL CHECK (length(body) BETWEEN 1 AND 1000),
    ip_hash         VARCHAR(64),
    user_agent      VARCHAR(200),
    like_count      INTEGER NOT NULL DEFAULT 0,
    reply_count     INTEGER NOT NULL DEFAULT 0,
    flags           TEXT,                                  -- JSON-as-text; we don't query it
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    hidden_at       TIMESTAMP,
    hidden_reason   VARCHAR(200),
    hidden_by       VARCHAR(64),
    CONSTRAINT chk_comment_identity CHECK (user_id IS NOT NULL OR guest_name IS NOT NULL)
);

CREATE INDEX idx_comments_property_created
    ON comments(property_id, created_at DESC) WHERE parent_id IS NULL;
CREATE INDEX idx_comments_parent ON comments(parent_id);
CREATE INDEX idx_comments_user ON comments(user_id);
CREATE INDEX idx_comments_ip_hash_created ON comments(ip_hash, created_at);
CREATE INDEX idx_comments_hidden ON comments(hidden_at) WHERE hidden_at IS NOT NULL;

CREATE TABLE comment_likes (
    comment_id  BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    identity    VARCHAR(64) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, identity)
);
CREATE INDEX idx_comment_likes_identity ON comment_likes(identity);

CREATE TABLE comment_reports (
    id           BIGSERIAL PRIMARY KEY,
    comment_id   BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reporter     VARCHAR(64) NOT NULL,
    reason       VARCHAR(40) NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP,
    resolved_by  VARCHAR(64),
    CONSTRAINT uq_report_per_reporter UNIQUE (comment_id, reporter)
);
CREATE INDEX idx_comment_reports_unresolved ON comment_reports(resolved_at)
    WHERE resolved_at IS NULL;
