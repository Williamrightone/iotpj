# PRD-004：IoT 資料設計（Topic / Event / Telemetry）

> 版本：v0.3
> 日期：2026-08-12
> 狀態：草稿

本文件定義 IoT 資料平面的訊息結構，涵蓋 MQTT topic 命名、Kafka topic 分工、訊息 Envelope 格式，以及各類 Device Event 的 Payload 規格。業務實體（批號、板件、告警等）的資料設計詳見各對應 PRD。

---

## 1. MQTT Topic 設計（IoT 裝置 → EMQX）

IoT 裝置向 EMQX 發布訊息時使用的 topic 命名規則：

```
willthx/{tenantId}/{dataType}/{stationId}/{machineId}
```

| 欄位 | 說明 | 範例 |
|------|------|------|
| `tenantId` | 租戶識別碼 | `tenant-001` |
| `dataType` | 資料類型 | `telemetry` / `event` |
| `stationId` | 站點代碼 | `S01` |
| `machineId` | 機台代碼（站點層級 IoT 填 `ENV` 或 `STATION`） | `M01` / `ENV` |

**範例：**
```
willthx/tenant-001/telemetry/S01/M01      ← 印刷機-01 感測數值
willthx/tenant-001/telemetry/S05/ENV      ← 廠務環境感測
willthx/tenant-001/event/S01/M01          ← 印刷機-01 製程事件
willthx/tenant-001/event/S02/M03          ← AOI機-01 製程事件
```

---

## 2. Kafka Topic 設計（iot-adapter → 內部服務）

iot-adapter 訂閱 EMQX、正規化後發佈至 Kafka：

```
{dataType}.{stationId}.{machineId}
```

### 2.1 Topic 清單

| Kafka Topic | Partition Key | 說明 | Consumer Group |
|-------------|--------------|------|---------------|
| `telemetry.{stationId}.{machineId}` | `machineId` | 感測器連續數值 | `telemetry-svc`、`realtime-bff` |
| `device-events.{stationId}` | `machineId` | 製程事件（上下料、批號、停機） | `iotcore-svc` |

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
| `eventType` | String | 事件類型（見第 5 節） |
| `schemaVer` | String | Payload schema 版本，供向後相容 |
| `tenantId` | String | 租戶識別碼 |
| `stationId` | String | 所屬站點 |
| `machineId` | String | 所屬機台 |
| `timestamp` | ISO8601 | 事件發生時間（裝置端時間） |
| `payload` | Object | 事件類型專屬資料 |

---

## 4. Telemetry Payload

`eventType` = `TELEMETRY`，高頻感測數值，每個 IoT Component 獨立一筆：

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

> 不合併多個感測值，每筆獨立訊息，schema 變更影響最小。

---

## 5. Device Event Payload

### 5.1 事件類型清單

| eventType | 觸發時機 | 觸發者 |
|-----------|---------|--------|
| `LOT_STARTED` | 批號投料 | 操作員 |
| `LOT_CLOSED` | 批號全數出站 | 操作員 / 系統 |
| `UNIT_LOADED` | 板件上料（進站） | 操作員 |
| `UNIT_UNLOADED` | 板件下料（出站） | 操作員 |
| `AOI_RESULT` | AOI 機器判定完成 | AOI 機台 |
| `FINAL_TEST_RESULT` | 最終組裝功能測試完成 | 測試設備 |
| `MACHINE_STOPPED` | 設備停機（換料/故障） | 設備 |
| `MACHINE_RESUMED` | 設備復工 | 操作員 |

> **AOI 複判（REVIEW_RESULT）** 由操作員透過 Web UI 觸發，走 REST API（saas-bff → iotcore），不經過 Kafka device-events。

### 5.2 各事件 Payload

**LOT_STARTED**
```json
{
  "lotId":       "Lot-007",
  "productNo":   "PCB-A100",
  "plannedQty":  20,
  "operatorId":  "operator-001"
}
```

**LOT_CLOSED**
```json
{
  "lotId":       "Lot-007",
  "operatorId":  "operator-001",
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

## 6. Telemetry 時序資料（TimescaleDB）

高頻感測資料儲存於 TimescaleDB，以 `time` 為分區鍵（Hypertable）。

**核心欄位：**

| 欄位 | 說明 |
|------|------|
| `time` | 資料時間戳（Hypertable 分區鍵） |
| `tenant_id` | 租戶識別碼 |
| `station_id` | 站點代碼 |
| `machine_id` | 機台代碼 |
| `component_code` | IoT 元件代碼 |
| `value` | 感測數值 |
| `unit` | 數值單位（kgf / °C / mm 等） |

**效能設計：**
- 複合索引 `(machine_id, component_code, time DESC)`，加速單機台歷史查詢
- Continuous Aggregate 預聚合每分鐘 avg/max/min，供 Dashboard 摘要查詢
- 原始資料保留 30 天，聚合資料保留 1 年

---

## 7. MQTT → Kafka 命名對照

| MQTT Topic（裝置發布） | Kafka Topic（iot-adapter 轉發） |
|----------------------|-------------------------------|
| `willthx/tenant-001/telemetry/S01/M01` | `telemetry.S01.M01` |
| `willthx/tenant-001/telemetry/S05/ENV` | `telemetry.S05.ENV` |
| `willthx/tenant-001/event/S01/M01` | `device-events.S01` |
| `willthx/tenant-001/event/S02/M03` | `device-events.S02` |
