# PRD-004：資料設計（Topic / Event / 料號 / 批號）

> 版本：v0.1
> 日期：2026-08-08
> 狀態：草稿

---

## 1. MQTT Topic 設計（IoT 裝置 → EMQX）

IoT 裝置向 EMQX 發布訊息時使用的 MQTT topic 命名規則：

```
willthx/{tenantId}/{dataType}/{stationId}/{machineId}
```

| 欄位 | 說明 | 範例 |
|------|------|------|
| `tenantId` | 租戶識別碼 | `tenant-001` |
| `dataType` | 資料類型 | `telemetry` / `event` |
| `stationId` | 站點代碼 | `S01` |
| `machineId` | 機台代碼（站點層級 IoT 省略） | `M01` / `ENV`（廠務） |

**範例：**
```
willthx/tenant-001/telemetry/S01/M01      ← 印刷機-01 感測數值
willthx/tenant-001/telemetry/S05/ENV      ← 廠務環境感測
willthx/tenant-001/event/S01/M01          ← 印刷機-01 製程事件
willthx/tenant-001/event/S02/M03          ← AOI機-01 製程事件
```

---

## 2. Kafka Topic 設計（iot-adapter → 內部服務）

iot-adapter 訂閱 EMQX、正規化後發佈至 Kafka，使用以下命名規則：

```
{dataType}.{stationId}.{machineId}
```

### 2.1 Topic 清單

| Kafka Topic | Partition Key | 說明 | Consumer Group |
|-------------|--------------|------|---------------|
| `telemetry.{stationId}.{machineId}` | `machineId` | 感測器連續數值 | `telemetry-svc`、`realtime-bff` |
| `device-events.{stationId}` | `machineId` | 製程事件（上下料、批號、停機）| `iotcore-svc` |

> **Partition Key = machineId**：確保同一台機器的訊息落在同一 partition，保持有序性。

### 2.2 Retention 策略

| Topic | Retention | 說明 |
|-------|----------|------|
| `telemetry.*` | 7 天 | 原始資料已持久化至 TimescaleDB |
| `device-events.*` | 30 天 | 支援 Saga replay 與問題排查 |

---

## 3. 訊息 Envelope（共用結構）

所有 MQTT 與 Kafka 訊息共用相同的 JSON 外層結構：

```json
{
  "eventId":    "550e8400-e29b-41d4-a716-446655440000",
  "eventType":  "UNIT_LOADED",
  "schemaVer":  "1.0",
  "tenantId":   "tenant-001",
  "stationId":  "S01",
  "machineId":  "M01",
  "timestamp":  "2026-08-08T10:03:00.000Z",
  "payload":    { ... }
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `eventId` | UUID | 冪等性 Key，iotcore 用來防止重複處理 |
| `eventType` | String | 事件類型（見第 4 節） |
| `schemaVer` | String | Payload schema 版本，供向後相容 |
| `tenantId` | String | 租戶識別碼 |
| `stationId` | String | 所屬站點 |
| `machineId` | String | 所屬機台（站點層級事件填 `ENV` 或 `STATION`） |
| `timestamp` | ISO8601 | 事件發生時間（裝置端時間） |
| `payload` | Object | 事件類型專屬資料 |

---

## 4. Telemetry Payload

`eventType` = `TELEMETRY`，高頻感測數值：

```json
{
  "eventId":       "uuid",
  "eventType":     "TELEMETRY",
  "schemaVer":     "1.0",
  "tenantId":      "tenant-001",
  "stationId":     "S01",
  "machineId":     "M01",
  "timestamp":     "2026-08-08T10:03:00.123Z",
  "payload": {
    "componentCode": "SP-PRESSURE",
    "value":         2.14,
    "unit":          "kgf"
  }
}
```

> 每個 IoT Component 獨立一筆訊息，不合併多個感測值。這樣 Kafka partition 可依 componentCode 進一步細分，且 schema 變更影響最小。

---

## 5. Device Event Payload

### 5.1 事件類型清單

| eventType | 觸發時機 | 觸發者 |
|-----------|---------|--------|
| `LOT_STARTED` | 批號投料 | 操作員 |
| `LOT_CLOSED` | 批號全數出站 | 操作員 / 系統 |
| `UNIT_LOADED` | 板件上料（進站） | 操作員 |
| `UNIT_UNLOADED` | 板件下料（出站）| 操作員 |
| `AOI_RESULT` | AOI 機器判定完成 | AOI 機台 |
| `REVIEW_RESULT` | 人員複判結果回寫 | 操作員（Web UI）|
| `FINAL_TEST_RESULT` | 最終組裝功能測試完成 | 測試設備 |
| `MACHINE_STOPPED` | 設備停機（換料/故障） | 設備 |
| `MACHINE_RESUMED` | 設備復工 | 操作員 |

### 5.2 各事件 Payload

**LOT_STARTED**
```json
{
  "lotId":      "Lot-007",
  "productNo":  "PCB-A100",
  "plannedQty": 20,
  "operatorId": "operator-001"
}
```

**LOT_CLOSED**
```json
{
  "lotId":      "Lot-007",
  "operatorId": "operator-001",
  "closeReason": "COMPLETED"
}
```

**UNIT_LOADED**
```json
{
  "unitSerial": "PCB-2026-00042",
  "lotId":      "Lot-007",
  "operatorId": "operator-001"
}
```

**UNIT_UNLOADED**
```json
{
  "unitSerial": "PCB-2026-00042",
  "lotId":      "Lot-007",
  "operatorId": "operator-001"
}
```

**AOI_RESULT**
```json
{
  "unitSerial":     "PCB-2026-00042",
  "lotId":          "Lot-007",
  "result":         "FAIL",
  "defectCode":     "SOLDER_BRIDGE",
  "defectLocation": "U12-PIN3"
}
```

**REVIEW_RESULT**
```json
{
  "unitSerial":   "PCB-2026-00042",
  "lotId":        "Lot-007",
  "reviewerId":   "operator-002",
  "reviewResult": "PASS",
  "reviewNote":   "目視確認無錫橋，誤判"
}
```

**FINAL_TEST_RESULT**
```json
{
  "unitSerial": "PCB-2026-00042",
  "lotId":      "Lot-007",
  "result":     "PASS"
}
```

**MACHINE_STOPPED**
```json
{
  "reason":     "MATERIAL_CHANGE",
  "operatorId": "operator-001",
  "note":       "換錫膏批次"
}
```

**MACHINE_RESUMED**
```json
{
  "operatorId": "operator-001"
}
```

---

## 6. 料號（Product）設計

料號識別 PCB 的產品型號，與批號為多對一關係（一個料號可以有多個批號）。

### 6.1 資料模型

```sql
CREATE TABLE products (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_no    VARCHAR(50) UNIQUE NOT NULL,   -- e.g., PCB-A100
  name          VARCHAR(200),
  description   TEXT,
  process_spec  JSONB,                         -- 製程規格（溫度曲線參數、扭力規格等）
  is_active     BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);
```

**process_spec 範例（JSONB）：**
```json
{
  "reflow": {
    "preheat_min": 120, "preheat_max": 180,
    "reflow_min":  220, "reflow_max":  260,
    "speed_min":   55,  "speed_max":   65
  },
  "torque": {
    "min": 0.8, "max": 1.6, "unit": "N·m"
  }
}
```

---

## 7. 批號（Lot）設計

### 7.1 資料模型

```sql
CREATE TABLE lots (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  lot_id        VARCHAR(50) UNIQUE NOT NULL,   -- e.g., Lot-007
  product_no    VARCHAR(50) NOT NULL REFERENCES products(product_no),
  planned_qty   INT NOT NULL,
  actual_qty    INT DEFAULT 0,
  pass_qty      INT DEFAULT 0,
  fail_qty      INT DEFAULT 0,
  status        VARCHAR(20) NOT NULL,          -- PENDING / ACTIVE / PAUSED / COMPLETING / COMPLETED
  started_at    TIMESTAMPTZ,
  closed_at     TIMESTAMPTZ,
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### 7.2 批號狀態（對應 Saga）

```
PENDING    ──LOT_STARTED──►  ACTIVE
ACTIVE     ──LOT_PAUSE───►  PAUSED
PAUSED     ──LOT_RESUME──►  ACTIVE
ACTIVE     ──LOT_CLOSED──►  COMPLETING ──► COMPLETED
```

---

## 8. Unit（板件）設計

### 8.1 資料模型

```sql
CREATE TABLE units (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  unit_serial   VARCHAR(100) UNIQUE NOT NULL,  -- e.g., PCB-2026-00042
  lot_id        VARCHAR(50) NOT NULL REFERENCES lots(lot_id),
  product_no    VARCHAR(50) NOT NULL,
  status        VARCHAR(50) NOT NULL,          -- 見 Unit 狀態機
  final_result  VARCHAR(10),                   -- PASS / FAIL / NULL（未完成）
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);
```

### 8.2 Unit 狀態機

```
PENDING
  → S01_IN_PROGRESS  （UNIT_LOADED @ S01）
  → S01_DONE         （UNIT_UNLOADED @ S01）
  → S02_IN_PROGRESS  （UNIT_LOADED @ S02）
  → S02_DONE         （UNIT_UNLOADED @ S02 + AOI PASS）
  → PENDING_REVIEW   （UNIT_UNLOADED @ S02 + AOI FAIL）
      → S02_DONE     （REVIEW_RESULT = PASS）
      → REJECTED     （REVIEW_RESULT = FAIL）
  → S03_IN_PROGRESS  （UNIT_LOADED @ S03）
  → S03_DONE         （UNIT_UNLOADED @ S03）
  → S04_IN_PROGRESS  （UNIT_LOADED @ S04）
  → COMPLETED        （FINAL_TEST_RESULT = PASS）
  → REJECTED         （FINAL_TEST_RESULT = FAIL）
```

### 8.3 跨站記錄（unit_station_records）

```sql
CREATE TABLE unit_station_records (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  unit_serial     VARCHAR(100) NOT NULL,
  lot_id          VARCHAR(50) NOT NULL,
  station_id      VARCHAR(20) NOT NULL,
  machine_id      VARCHAR(20) NOT NULL,
  operator_id     VARCHAR(50),
  loaded_at       TIMESTAMPTZ NOT NULL,
  unloaded_at     TIMESTAMPTZ,
  duration_sec    INT,                         -- unloaded_at - loaded_at
  result          VARCHAR(10),                 -- PASS / FAIL / WARN / NULL
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_usr_unit   ON unit_station_records(unit_serial);
CREATE INDEX idx_usr_lot    ON unit_station_records(lot_id);
CREATE INDEX idx_usr_loaded ON unit_station_records(loaded_at);
```

---

## 9. AOI 複判記錄

```sql
CREATE TABLE aoi_results (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  unit_serial      VARCHAR(100) NOT NULL,
  lot_id           VARCHAR(50) NOT NULL,
  machine_id       VARCHAR(20) NOT NULL,
  inspected_at     TIMESTAMPTZ NOT NULL,
  machine_result   VARCHAR(10) NOT NULL,        -- PASS / FAIL
  defect_code      VARCHAR(50),                 -- SOLDER_BRIDGE / MISSING_SOLDER / OFFSET
  defect_location  VARCHAR(100),
  -- 複判欄位
  reviewer_id      VARCHAR(50),
  reviewed_at      TIMESTAMPTZ,
  review_result    VARCHAR(10),                 -- PASS / FAIL
  review_note      TEXT,
  final_result     VARCHAR(10) NOT NULL,        -- 最終結果（複判後更新）
  created_at       TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 10. Saga 與冪等性 Tables

```sql
-- Lot Saga 狀態
CREATE TABLE lot_sagas (
  lot_id         VARCHAR(50) PRIMARY KEY,
  current_state  VARCHAR(30) NOT NULL,
  version        INT NOT NULL DEFAULT 0,        -- 樂觀鎖
  payload        JSONB,
  updated_at     TIMESTAMPTZ DEFAULT NOW()
);

-- Unit Saga 狀態
CREATE TABLE unit_sagas (
  unit_serial      VARCHAR(100) PRIMARY KEY,
  lot_id           VARCHAR(50) NOT NULL,
  current_state    VARCHAR(50) NOT NULL,
  current_station  VARCHAR(20),
  version          INT NOT NULL DEFAULT 0,
  payload          JSONB,
  updated_at       TIMESTAMPTZ DEFAULT NOW()
);

-- 已處理事件（冪等性）
CREATE TABLE processed_events (
  event_id       VARCHAR(100) PRIMARY KEY,
  processed_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Outbox（待發訊息）
CREATE TABLE outbox (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  destination    VARCHAR(200) NOT NULL,         -- Kafka topic 或 RabbitMQ queue
  payload        JSONB NOT NULL,
  published      BOOLEAN DEFAULT FALSE,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox(published, created_at)
  WHERE published = FALSE;
```

---

## 11. 告警 Tables

```sql
-- 告警規則
CREATE TABLE alert_rules (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  component_code  VARCHAR(50) NOT NULL,
  station_id      VARCHAR(20),
  machine_id      VARCHAR(20),
  condition_op    VARCHAR(5) NOT NULL,           -- GT / LT / GTE / LTE
  threshold       DECIMAL(12,4) NOT NULL,
  consecutive_n   INT DEFAULT 1,                 -- 連續 N 次才觸發
  severity        VARCHAR(10) NOT NULL,          -- HIGH / LOW
  notify_channels VARCHAR(100),                  -- dashboard / dashboard,telegram
  is_active       BOOLEAN DEFAULT TRUE,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 告警事件
CREATE TABLE alert_events (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  rule_id          UUID REFERENCES alert_rules(id),
  station_id       VARCHAR(20),
  machine_id       VARCHAR(20),
  component_code   VARCHAR(50),
  triggered_value  DECIMAL(12,4),
  threshold        DECIMAL(12,4),
  severity         VARCHAR(10) NOT NULL,
  triggered_at     TIMESTAMPTZ NOT NULL,
  status           VARCHAR(20) DEFAULT 'OPEN',   -- OPEN / ACKNOWLEDGED
  acked_by         VARCHAR(50),
  acked_at         TIMESTAMPTZ,
  ack_note         TEXT,
  created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ae_triggered  ON alert_events(triggered_at DESC);
CREATE INDEX idx_ae_status     ON alert_events(status);
```

---

## 12. TimescaleDB Telemetry Table

```sql
-- Hypertable（時序主表）
CREATE TABLE telemetry (
  time            TIMESTAMPTZ NOT NULL,
  tenant_id       VARCHAR(50) NOT NULL,
  station_id      VARCHAR(20) NOT NULL,
  machine_id      VARCHAR(20) NOT NULL,
  component_code  VARCHAR(50) NOT NULL,
  value           DECIMAL(12,4) NOT NULL,
  unit            VARCHAR(20)
);

-- 轉為 TimescaleDB Hypertable（依 time 分 chunk，每 1 天一個 chunk）
SELECT create_hypertable('telemetry', 'time', chunk_time_interval => INTERVAL '1 day');

-- 複合索引（常用查詢路徑）
CREATE INDEX idx_tel_machine_time
  ON telemetry(machine_id, component_code, time DESC);

-- Continuous Aggregate（預聚合，供 Dashboard 摘要查詢）
CREATE MATERIALIZED VIEW telemetry_1min
WITH (timescaledb.continuous) AS
SELECT
  time_bucket('1 minute', time) AS bucket,
  machine_id,
  component_code,
  AVG(value) AS avg_val,
  MAX(value) AS max_val,
  MIN(value) AS min_val
FROM telemetry
GROUP BY bucket, machine_id, component_code;

-- Retention（原始資料 30 天，聚合資料不受限）
SELECT add_retention_policy('telemetry', INTERVAL '30 days');
```

---

## 13. 資料關聯總覽

```
products (料號)
    └── lots (批號) ── lot_sagas
          └── units (板件) ── unit_sagas
                └── unit_station_records (跨站記錄)
                └── aoi_results (AOI 複判)

iot_components (IoT 元件設定)
    └── alert_rules (告警規則)
          └── alert_events (告警歷史)

telemetry (TimescaleDB)
    → 透過 machine_id + time 與 unit_station_records.loaded_at/unloaded_at 關聯

outbox / processed_events
    → Saga 輔助表，不參與業務查詢
```

---

## 14. MQTT → Kafka 命名對照

| MQTT Topic（裝置發布）| Kafka Topic（iot-adapter 轉發）|
|----------------------|-------------------------------|
| `willthx/tenant-001/telemetry/S01/M01` | `telemetry.S01.M01` |
| `willthx/tenant-001/telemetry/S05/ENV` | `telemetry.S05.ENV` |
| `willthx/tenant-001/event/S01/M01` | `device-events.S01` |
| `willthx/tenant-001/event/S02/M03` | `device-events.S02` |
