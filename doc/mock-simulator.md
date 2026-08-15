# IoT Mock Simulator

> 版本：v0.1
> 日期：2026-08-14
> 狀態：草稿

模擬真實 IoT 裝置（ESP32、PLC Gateway、AOI Gateway）向 EMQX 發布 MQTT 訊息，用於：
- **功能驗證**：在硬體到位前先驗證後端 Pipeline 正確性
- **壓力測試**：透過頻率壓縮達到百萬流量等級

---

## 1. 架構

```
mock-simulator/
├── config.yaml              ← 站點 / 機台 / 元件設定（對應 PRD-4）
├── main.py                  ← 進入點，啟動所有 coroutine
├── devices/
│   ├── base.py              ← MqttDevice 基底類別
│   ├── esp32.py             ← Type A：固定頻率 Telemetry publish
│   ├── plc_gateway.py       ← Type B：Modbus Gateway 模擬（事件驅動 + Telemetry）
│   └── system_gateway.py    ← Type C：封閉系統 Gateway（AOI、ICT 事件）
├── process/
│   └── orchestrator.py      ← 控制 LOT / UNIT 生命週期，依序觸發 Event
├── telemetry/
│   └── generator.py         ← 產生有波動的感測值（正態分佈 + 偶發異常）
└── utils/
    └── mqtt_client.py       ← paho-mqtt wrapper（連線 / reconnect / publish）
```

**運作流程：**

```
main.py
  ├── asyncio.gather(
  │     esp32_simulator(S01-M01),   ← 每台機器一個 coroutine
  │     esp32_simulator(S01-M02),
  │     ...（40 個 Telemetry coroutine）
  │     process_orchestrator(),      ← 1 個 coroutine 控制所有 LOT/UNIT Event
  │   )
  └── 所有 coroutine 並發執行
```

---

## 2. 裝置類型行為

### Type A — ESP32 直發型
- 固定頻率 publish Telemetry
- 每個 `componentCode` 獨立 topic
- 感測值由 `generator.py` 產生（正態分佈 + 偶發超標）

### Type B — PLC / Modbus Gateway
- Telemetry 行為同 Type A（模擬 Gateway 讀取工業感測器後轉發）
- 額外負責機台停機 / 復工 Event（`MACHINE_STOPPED` / `MACHINE_RESUMED`）

### Type C — 封閉系統 Gateway（AOI、ICT）
- 純事件驅動，無連續 Telemetry
- 由 Orchestrator 控制觸發時機，Gateway 模擬解析後 publish Event

---

## 3. 設定檔（config.yaml）

```yaml
mqtt:
  host: localhost
  port: 1883
  username: ""
  password: ""

tenant_id: tenant-001

stress:
  enabled: false
  frequency_multiplier: 10   # 10x → 原本 1s 間隔壓縮至 100ms

process:
  lot_id_prefix: "Lot-"
  unit_serial_prefix: "PCB-2026-"
  lot_size: 20               # 每批號板件數
  operator_ids: ["OP-001", "OP-002", "OP-003"]
  aoi_fail_rate: 0.05        # AOI 缺陷率 5%
  final_test_fail_rate: 0.02 # 電測不良率 2%

stations:
  - id: S01
    name: 錫膏印刷
    device_type: A
    machines:
      count: 10
      id_prefix: S01-M
    components:
      - code: SP-PRESSURE
        unit: kgf
        interval_sec: 1
        normal: { mean: 10.0, std: 0.5, min: 6.0, max: 14.0 }
        anomaly: { rate: 0.01, range: [14.0, 18.0] }
      - code: SP-THICKNESS
        unit: mm
        interval_sec: 1
        normal: { mean: 0.15, std: 0.01, min: 0.10, max: 0.20 }
        anomaly: { rate: 0.01, range: [0.20, 0.25] }
      - code: SP-SPEED
        unit: mm/s
        interval_sec: 1
        normal: { mean: 80.0, std: 5.0, min: 50.0, max: 110.0 }
        anomaly: { rate: 0.005, range: [110.0, 130.0] }

  - id: S02
    name: 回流焊
    device_type: B
    machines:
      count: 10
      id_prefix: S02-M
    components:
      - code: RF-TEMP-PREHEAT
        unit: "°C"
        interval_sec: 2
        normal: { mean: 165.0, std: 3.0, min: 150.0, max: 180.0 }
        anomaly: { rate: 0.005, range: [180.0, 200.0] }
      - code: RF-TEMP-SOAK
        unit: "°C"
        interval_sec: 2
        normal: { mean: 195.0, std: 3.0, min: 180.0, max: 210.0 }
        anomaly: { rate: 0.005, range: [210.0, 230.0] }
      - code: RF-TEMP-REFLOW
        unit: "°C"
        interval_sec: 2
        normal: { mean: 250.0, std: 2.0, min: 240.0, max: 260.0 }
        anomaly: { rate: 0.005, range: [260.0, 275.0] }
      - code: RF-TEMP-COOL
        unit: "°C"
        interval_sec: 2
        normal: { mean: 75.0, std: 5.0, min: 50.0, max: 100.0 }
        anomaly: { rate: 0.003, range: [100.0, 120.0] }
      - code: RF-BELT-SPEED
        unit: cm/min
        interval_sec: 5
        normal: { mean: 65.0, std: 1.0, min: 60.0, max: 70.0 }
        anomaly: { rate: 0.002, range: [70.0, 80.0] }

  - id: S03
    name: AOI 光學檢測
    device_type: C
    machines:
      count: 10
      id_prefix: S03-M
    components: []            # 無 Telemetry，純 Event

  - id: S04
    name: 電測／最終組裝
    device_type: A
    machines:
      count: 10
      id_prefix: S04-M
    components:
      - code: FA-TORQUE
        unit: "N·m"
        interval_sec: null    # 事件驅動，每鎖點觸發
        normal: { mean: 0.50, std: 0.03, min: 0.40, max: 0.60 }
        anomaly: { rate: 0.02, range: [0.60, 0.75] }

  - id: S05
    name: 廠務環境
    device_type: A
    machines: null            # 站點層級，無機台
    components:
      - code: ENV-TEMP
        unit: "°C"
        interval_sec: 30
        normal: { mean: 23.0, std: 0.5, min: 20.0, max: 26.0 }
        anomaly: { rate: 0.002, range: [26.0, 30.0] }
      - code: ENV-HUMIDITY
        unit: "%"
        interval_sec: 30
        normal: { mean: 50.0, std: 3.0, min: 40.0, max: 60.0 }
        anomaly: { rate: 0.002, range: [60.0, 70.0] }
      - code: ENV-POWER
        unit: kW
        interval_sec: 30
        normal: { mean: 45.0, std: 5.0, min: 30.0, max: 65.0 }
        anomaly: { rate: 0.001, range: [65.0, 80.0] }
```

---

## 4. Telemetry 數值範圍

| componentCode | 正常範圍 | 平均 | 標準差 | 異常閾值 | 異常發生率 |
|--------------|---------|------|-------|---------|---------|
| SP-PRESSURE | 6–14 kgf | 10.0 | 0.5 | > 14.0 | 1% |
| SP-THICKNESS | 0.10–0.20 mm | 0.15 | 0.01 | > 0.20 | 1% |
| SP-SPEED | 50–110 mm/s | 80.0 | 5.0 | > 110.0 | 0.5% |
| RF-TEMP-PREHEAT | 150–180 °C | 165.0 | 3.0 | > 180.0 | 0.5% |
| RF-TEMP-SOAK | 180–210 °C | 195.0 | 3.0 | > 210.0 | 0.5% |
| RF-TEMP-REFLOW | 240–260 °C | 250.0 | 2.0 | > 260.0 | 0.5% |
| RF-TEMP-COOL | 50–100 °C | 75.0 | 5.0 | > 100.0 | 0.3% |
| RF-BELT-SPEED | 60–70 cm/min | 65.0 | 1.0 | > 70.0 | 0.2% |
| FA-TORQUE | 0.40–0.60 N·m | 0.50 | 0.03 | > 0.60 | 2% |
| ENV-TEMP | 20–26 °C | 23.0 | 0.5 | > 26.0 | 0.2% |
| ENV-HUMIDITY | 40–60 % | 50.0 | 3.0 | > 60.0 | 0.2% |
| ENV-POWER | 30–65 kW | 45.0 | 5.0 | > 65.0 | 0.1% |

---

## 5. Process Orchestrator — LOT / UNIT 流程

### LOT 生命週期

```
Orchestrator 啟動
  → 建立 Lot（publish LOT_STARTED @ S01）
  → 逐批分配 Unit 到各站機台
  → 等待所有 Unit 完成 S04
  → publish LOT_CLOSED @ S04
  → 建立下一個 Lot（循環）
```

### UNIT 流程（每站）

```
分配 Unit 到機台
  → publish UNIT_LOADED @ Sxx
  → 等待製程時間（sleep，可壓縮）
  → S03：決定 AOI 結果（依 aoi_fail_rate）
      PASS → publish AOI_RESULT(PASS) + UNIT_UNLOADED
      FAIL → publish AOI_RESULT(FAIL) + UNIT_UNLOADED
             （複判邏輯由 Web UI 操作，Mock 不模擬）
  → S04：決定電測結果（依 final_test_fail_rate）
      → publish FINAL_TEST_RESULT + UNIT_UNLOADED
  → 其他站：publish UNIT_UNLOADED
  → 進入下一站
```

### 各站製程時間（預設值，可調）

| 站點 | 預設製程時間 | 說明 |
|------|-----------|------|
| S01 | 8s | 印刷機週期 |
| S02 | 15s | 爐體通過時間（壓縮模擬）|
| S03 | 5s | AOI 掃描時間 |
| S04 | 10s | 電測 + 組裝時間 |

> 正式場景：各站 10 分鐘。Mock 預設大幅壓縮以加快驗證速度。

---

## 6. 壓測模式

在 `config.yaml` 開啟：

```yaml
stress:
  enabled: true
  frequency_multiplier: 10   # Telemetry 頻率 × 10
```

**效果：**

| 參數 | 正常模式 | 壓測 10x |
|------|---------|---------|
| S01 Telemetry | 30 msg/s | 300 msg/s |
| S02 Telemetry | 22 msg/s | 220 msg/s |
| 全站合計 | ~52 msg/s | ~528 msg/s |
| 每小時 | ~19 萬筆 | ~190 萬筆 |

`frequency_multiplier` 只影響 Telemetry 間隔，不影響 Event 頻率（Event 由製程時間決定）。

---

## 7. 依賴套件

```
paho-mqtt>=1.6.1
pyyaml>=6.0
numpy>=1.24         # 正態分佈數值生成
asyncio             # Python 標準庫
```

---

## 8. 執行方式

```bash
cd mock-simulator
pip install -r requirements.txt

# 正常模式
python main.py

# 壓測模式（覆蓋 config）
python main.py --stress --multiplier 10

# 只跑特定站點
python main.py --stations S01 S02
```

---

## 9. MQTT 連線設定

Mock 連線至 EMQX，可使用 docker-compose 起的本地實例：

| 項目 | 預設值 |
|------|-------|
| Host | localhost |
| Port | 1883 |
| Protocol | MQTT 3.1.1 |
| QoS | 1（Telemetry），1（Event）|
| Client ID | `mock-{deviceType}-{machineId}` |

> QoS 1 確保 at-least-once delivery，iotcore-service 依靠 idempotency key（processed_events）去重。
