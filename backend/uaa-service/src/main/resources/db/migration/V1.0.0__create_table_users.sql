-- willThx UAA Service
-- V1.0.0 : 建立 users 資料表

CREATE TABLE users
(
    id             BIGINT       NOT NULL           COMMENT 'Snowflake ID',
    username       VARCHAR(64)  NOT NULL,
    name           VARCHAR(128) NOT NULL,
    password_hash  VARCHAR(128) NOT NULL           COMMENT 'BCrypt 雜湊',
    role           VARCHAR(20)  NOT NULL           COMMENT 'ADMIN / MAINTAINER / VIEWER',
    tenant_id      BIGINT                          COMMENT '所屬租戶 ID；null 表示平台層超管',
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at  DATETIME(3),
    created_at     DATETIME(3)  NOT NULL,
    updated_at     DATETIME(3)  NOT NULL,
    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_username    UNIQUE (username)
) COMMENT = '使用者帳號（跨租戶）';

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_username   ON users (username);
