-- willThx UAA Service
-- V1.0.0 : 建立 users 資料表（含最終欄位，無需 ALTER）

CREATE TABLE users
(
    id            BIGINT       NOT NULL           COMMENT 'Snowflake ID',
    account       VARCHAR(100) NOT NULL           COMMENT 'Email 格式登入帳號，全系統唯一',
    display_name  VARCHAR(100) NOT NULL           COMMENT '顯示姓名',
    password_hash VARCHAR(128) NOT NULL           COMMENT 'BCrypt 雜湊',
    role          VARCHAR(30)  NOT NULL           COMMENT 'ADMIN / MAINTAINER / VIEWER',
    tenant_id     BIGINT                          COMMENT '所屬租戶 ID；null 表示平台層超管',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
    last_login_at DATETIME(3),
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uq_users_account UNIQUE (account)
) COMMENT = '使用者帳號（跨租戶）';

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_account   ON users (account);
