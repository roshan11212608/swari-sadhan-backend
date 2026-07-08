-- Refresh token storage for JWT refresh flow.
-- Tokens are stored as SHA-256 hashes; raw tokens never touch the database.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    token_hash  VARCHAR(64)  NOT NULL,
    user_email  VARCHAR(150) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_rt_token_hash (token_hash),
    KEY idx_rt_user_email (user_email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
