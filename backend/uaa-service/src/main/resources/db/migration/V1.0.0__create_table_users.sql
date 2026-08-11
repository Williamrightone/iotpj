-- willThx UAA Service
-- V1.0.0 : 建立 users 資料表

CREATE TABLE users
(
    id             BIGINT       NOT NULL,
    username       VARCHAR(64)  NOT NULL,
    name           VARCHAR(128) NOT NULL,
    password_hash  VARCHAR(128) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    tenant_id      BIGINT,                          -- null = 平台層超級管理員
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_username    UNIQUE (username)
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_username   ON users (username);

COMMENT ON TABLE  users                IS '使用者帳號（跨租戶）';
COMMENT ON COLUMN users.id            IS 'Snowflake ID';
COMMENT ON COLUMN users.tenant_id     IS '所屬租戶 ID；null 表示平台層超管';
COMMENT ON COLUMN users.role          IS 'ADMIN / OPERATOR / VIEWER';
COMMENT ON COLUMN users.password_hash IS 'BCrypt 雜湊';
