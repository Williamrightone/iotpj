-- willThx UAA Service
-- V1.0.2 : 建立 user_station_bindings 資料表
--   記錄 Maintainer / Viewer 可存取的製程站點範圍
--   Admin 此表無記錄（程式碼層保證）
--   無記錄（stationIds 空）= 可存取全部站點

CREATE TABLE user_station_bindings
(
    id         BIGINT       NOT NULL           COMMENT 'Snowflake ID',
    user_id    BIGINT       NOT NULL           COMMENT '所屬使用者 ID',
    station_id VARCHAR(64)  NOT NULL           COMMENT '站點代碼字串（如 S01），跨服務不做 DB FK',
    tenant_id  BIGINT       NOT NULL           COMMENT '多租戶隔離',
    created_at DATETIME(3)  NOT NULL,
    updated_at DATETIME(3)  NOT NULL,
    CONSTRAINT pk_user_station_bindings        PRIMARY KEY (id),
    CONSTRAINT uq_user_station                 UNIQUE (user_id, station_id)
) COMMENT = 'Maintainer / Viewer 的製程站點範圍綁定';

CREATE INDEX idx_usb_user_id   ON user_station_bindings (user_id);
CREATE INDEX idx_usb_tenant_id ON user_station_bindings (tenant_id);
