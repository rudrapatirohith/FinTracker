-- Stores short-lived, single-use codes used to complete OAuth login
-- between the backend and the frontend.
--
-- The raw handoff code is never stored.
-- The raw refresh token is never stored.

CREATE TABLE oauth_login_handoffs (
    id UUID PRIMARY KEY,

    code_hash VARCHAR(64) NOT NULL,

    user_id UUID NOT NULL,

    refresh_token_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMPTZ NOT NULL,

    consumed_at TIMESTAMPTZ,

    CONSTRAINT uq_oauth_login_handoffs_code_hash
        UNIQUE (code_hash),

    CONSTRAINT fk_oauth_login_handoffs_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_oauth_handoffs_code_hash
        CHECK (code_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT chk_oauth_handoffs_refresh_hash
        CHECK (refresh_token_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT chk_oauth_handoffs_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_oauth_login_handoffs_user_id
    ON oauth_login_handoffs(user_id);

-- Supports cleanup of expired, unused handoff records.
CREATE INDEX idx_oauth_login_handoffs_expiration
    ON oauth_login_handoffs(expires_at)
    WHERE consumed_at IS NULL;
