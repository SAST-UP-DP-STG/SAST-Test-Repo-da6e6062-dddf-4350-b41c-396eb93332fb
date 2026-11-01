CREATE TABLE IF NOT EXISTS app_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS password_reset_token (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    token VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL,
    expires_at VARCHAR(50) NOT NULL,
    used_at VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_user_id ON password_reset_token(user_id);
