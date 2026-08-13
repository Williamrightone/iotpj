-- willThx UAA Service
-- V1.0.1 : users 表欄位重構，對齊 PRD-005 / SPEC-1 設計
--   username  → account  (Email 格式，全系統唯一)
--   name      → display_name
--   active    → status   (VARCHAR 'ACTIVE' / 'DISABLED')

-- 1. 新增目標欄位（暫時允許 NULL 以便資料遷移）
ALTER TABLE users
    ADD COLUMN account      VARCHAR(100),
    ADD COLUMN display_name VARCHAR(100),
    ADD COLUMN status       VARCHAR(20);

-- 2. 遷移現有資料
UPDATE users
SET account      = username,
    display_name = name,
    status       = CASE WHEN active THEN 'ACTIVE' ELSE 'DISABLED' END;

-- 3. 設為 NOT NULL
ALTER TABLE users
    MODIFY COLUMN account      VARCHAR(100) NOT NULL COMMENT 'Email 格式登入帳號，全系統唯一',
    MODIFY COLUMN display_name VARCHAR(100) NOT NULL COMMENT '顯示姓名',
    MODIFY COLUMN status       VARCHAR(20)  NOT NULL COMMENT 'ACTIVE / DISABLED';

-- 4. 移除舊欄位
ALTER TABLE users
    DROP COLUMN username,
    DROP COLUMN name,
    DROP COLUMN active;

-- 5. 更新唯一約束（原為全域唯一，維持全域唯一語義）
ALTER TABLE users DROP INDEX uq_users_username;
ALTER TABLE users ADD  CONSTRAINT uq_users_account UNIQUE (account);

-- 6. 更新索引
DROP INDEX idx_users_username ON users;
CREATE INDEX idx_users_account ON users (account);
