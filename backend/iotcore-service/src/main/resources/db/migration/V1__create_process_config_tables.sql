CREATE TABLE stations (
  id           BIGINT       NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  tenant_id    BIGINT       NOT NULL COMMENT '租戶 ID',
  station_code VARCHAR(64)  NOT NULL COMMENT '站點代碼（不可修改）',
  name         VARCHAR(100) NOT NULL COMMENT '站點名稱',
  description  VARCHAR(255)         COMMENT '說明',
  sort_order   INT          NOT NULL DEFAULT 0 COMMENT '排序',
  is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否啟用',
  created_at   DATETIME(3)  NOT NULL,
  updated_at   DATETIME(3)  NOT NULL,
  UNIQUE KEY uq_station_code (tenant_id, station_code),
  INDEX idx_station_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE machines (
  id           BIGINT       NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  tenant_id    BIGINT       NOT NULL COMMENT '租戶 ID',
  station_id   BIGINT       NOT NULL COMMENT 'FK stations.id',
  machine_code VARCHAR(64)  NOT NULL COMMENT '機台代碼（不可修改）',
  name         VARCHAR(100) NOT NULL COMMENT '機台名稱',
  model        VARCHAR(100)         COMMENT '型號備註',
  is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否啟用',
  created_at   DATETIME(3)  NOT NULL,
  updated_at   DATETIME(3)  NOT NULL,
  UNIQUE KEY uq_machine_code (tenant_id, machine_code),
  INDEX idx_machine_station (station_id),
  INDEX idx_machine_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iot_components (
  id                  BIGINT        NOT NULL PRIMARY KEY COMMENT 'Snowflake ID',
  tenant_id           BIGINT        NOT NULL COMMENT '租戶 ID',
  station_id          BIGINT        NOT NULL COMMENT 'FK stations.id',
  machine_id          BIGINT                 COMMENT 'FK machines.id；NULL = 站點層級',
  component_code      VARCHAR(64)   NOT NULL COMMENT '元件代碼（不可修改）',
  name                VARCHAR(100)  NOT NULL COMMENT '元件名稱',
  data_type           VARCHAR(20)   NOT NULL COMMENT 'TELEMETRY / EVENT',
  unit                VARCHAR(30)            COMMENT 'TELEMETRY 必填',
  report_interval_sec INT                    COMMENT '上報頻率秒數；0 = 事件驅動',
  normal_upper        DECIMAL(15,4)          COMMENT '正常上限',
  normal_lower        DECIMAL(15,4)          COMMENT '正常下限',
  is_active           TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '是否啟用',
  created_at          DATETIME(3)   NOT NULL,
  updated_at          DATETIME(3)   NOT NULL,
  INDEX idx_comp_machine (machine_id),
  INDEX idx_comp_station (station_id),
  INDEX idx_comp_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
