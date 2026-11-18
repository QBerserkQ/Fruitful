CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY
    , user_a_id BIGINT NOT NULL REFERENCES users(id)
    , user_b_id BIGINT NOT NULL REFERENCES users(id)
    , user_a_last_read_at TIMESTAMPTZ
    , user_b_last_read_at TIMESTAMPTZ
    , created_at TIMESTAMPTZ NOT NULL DEFAULT now()
    , CONSTRAINT chk_conversations_user_order CHECK ( user_a_id < user_b_id )
    , CONSTRAINT uq_conversations_pair UNIQUE (user_a_id, user_b_id)
);

CREATE INDEX idx_conversations_user_b ON conversations (user_b_id);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY
    , conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE
    , sender_id BIGINT NOT NULL REFERENCES users(id)
    , content TEXT NOT NULL
    , created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_created ON messages (conversation_id, created_at DESC);