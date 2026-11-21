CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY
    , author_id BIGINT NOT NULL REFERENCES users(id)
    , content TEXT NOT NULL CHECK ( char_length(content) BETWEEN 1 AND 5000)
    , created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_posts_author_by_created_at ON posts (author_id, created_at DESC);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);