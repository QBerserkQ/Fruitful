DROP TABLE IF EXISTS refresh_tokens;

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY
    , user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
    , token_hash VARCHAR(64) NOT NULL
    , user_agent VARCHAR(255)
    , created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    , last_used_at TIMESTAMPTZ NOT NULL DEFAULT now()
    , expires_at TIMESTAMPTZ NOT NULL
    , CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_last_used
    ON refresh_tokens (user_id, last_used_at);