-- willThx UAA Service
-- V1.0.4 : 建立 role_feature_permissions 資料表
--   記錄 MAINTAINER / VIEWER 可存取的子功能
--   ADMIN 永遠擁有全部功能，程式碼層跳過此表，DB 不存 ADMIN 記錄
--   只對子功能（features.parent_id IS NOT NULL）設定，父功能由應用層推算

CREATE TABLE role_feature_permissions
(
    id         BIGINT      NOT NULL           COMMENT 'Snowflake ID',
    tenant_id  BIGINT      NOT NULL           COMMENT '多租戶隔離',
    role       VARCHAR(30) NOT NULL           COMMENT 'MAINTAINER 或 VIEWER',
    feature_id BIGINT      NOT NULL           COMMENT '子功能 ID（features.parent_id IS NOT NULL）',
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_role_feature_permissions     PRIMARY KEY (id),
    CONSTRAINT uq_role_feature                 UNIQUE (tenant_id, role, feature_id)
) COMMENT = '角色 × 子功能授權矩陣，僅存 MAINTAINER / VIEWER';

CREATE INDEX idx_rfp_tenant_role ON role_feature_permissions (tenant_id, role);
CREATE INDEX idx_rfp_feature_id  ON role_feature_permissions (feature_id);
