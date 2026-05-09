-- Create news_articles table
CREATE TABLE news_articles (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(300) NOT NULL,
    summary       VARCHAR(500) NOT NULL,
    content       TEXT NOT NULL,
    author        VARCHAR(100),
    category      VARCHAR(80),
    image_url     VARCHAR(1000),
    published_at  TIMESTAMP,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_articles_published_at ON news_articles(published_at DESC);
CREATE INDEX idx_news_articles_category ON news_articles(category);

COMMENT ON TABLE news_articles IS 'Real estate market news articles';
