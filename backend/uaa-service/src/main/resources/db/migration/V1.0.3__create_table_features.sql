-- willThx UAA Service
-- V1.0.3 : 建立 features 資料表
--   支援兩層樹狀：父功能群組（parent_id IS NULL）+ 子功能頁面（parent_id IS NOT NULL）
--   is_active=false 為軟停用；硬刪除須由應用層同步清理 role_feature_permissions

CREATE TABLE features
(
    id           BIGINT        NOT NULL           COMMENT 'Snowflake ID',
    tenant_id    BIGINT        NOT NULL           COMMENT '多租戶隔離',
    parent_id    BIGINT                           COMMENT 'null = 父功能群組；非 null = 子功能（實際頁面）',
    feature_code VARCHAR(100)  NOT NULL           COMMENT '功能識別碼，租戶內唯一',
    feature_name VARCHAR(100)  NOT NULL,
    route        VARCHAR(255)                     COMMENT '前端路由路徑；父功能為 null，子功能必填',
    sort_order   INT           NOT NULL DEFAULT 0 COMMENT '同層級排序，升冪',
    is_active    TINYINT(1)    NOT NULL DEFAULT 1 COMMENT 'false = 軟停用，所有角色不可見',
    created_at   DATETIME(3)   NOT NULL,
    updated_at   DATETIME(3)   NOT NULL,
    CONSTRAINT pk_features              PRIMARY KEY (id),
    CONSTRAINT uq_features_code         UNIQUE (tenant_id, feature_code)
) COMMENT = '動態功能清單，支援父子兩層結構';

CREATE INDEX idx_features_tenant_id ON features (tenant_id);
CREATE INDEX idx_features_parent_id ON features (tenant_id, parent_id);
