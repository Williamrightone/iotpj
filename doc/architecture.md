# willThx 系統架構文件

> 版本：v0.1｜對應 PRD：3-architecture.md

---

## 1. 專案概覽

willThx 是一套針對 PCB 製造業（HP、Lenovo、ASUS 等）的 SaaS IoT 平台。
核心目標：即時設備監控、製程告警、批號追溯、百萬級 MQTT 流量吞吐。

### 技術選型

| 層面 | 技術 |
|---|---|
| 語言 / 框架 | Java 21 + Spring Boot 3.x |
| 建構工具 | Maven（多模組） |
| API 閘道 | Nginx（部署於 Windows Server 2019） |
| 訊息佇列 | Kafka 4.x（KRaft，無 Zookeeper）+ RabbitMQ |
| MQTT Broker | EMQX |
| 時序資料庫 | TimescaleDB（獨立實例） |
| 業務資料庫 | PostgreSQL 16 |
| 快取 / Session | Redis 7 |
| 服務網格 / 部署 | Kubernetes（VMware VM 內）|
| GitOps | ArgoCD |
| 可觀測性 | Prometheus + Loki + Grafana |
| DevOps | Harbor（Image Registry）+ SonarQube |
| 前端 | 純 HTML/CSS/JS，部署於 Windows Server Nginx |

---

## 2. 整體部署架構

```
Internet / Factory IoT Devices
          │
          ▼
[ Windows Server 2019 ]
  ├── Nginx（外部反向代理 + 前端靜態檔）
  │     ├── /  -> prototype/ (HTML 前端)
  │     └── /api/* -> k8s Service (內部 IP)
  │
  └── VMware VM
        └── Kubernetes Cluster
              ├── uaa-service        :8081
              ├── iotcore-service    :8082
              ├── telemetry-service  :8083
              ├── iot-adapter        :8084
              ├── saas-bff           :8090
              └── realtime-bff       :8091
```

**EMQX** 部署於 Kubernetes，接收工廠設備 MQTT 連線（Port 1883/8883）。
**Kafka 4.x** 以 KRaft 模式運行於 Kubernetes（3 節點）。
**RabbitMQ** 運行於 Kubernetes，負責業務告警 Fanout。
**TimescaleDB** 獨立 StatefulSet，不與業務 PostgreSQL 共用實例。
**ArgoCD** 裝於 Kubernetes VM 內，監聽 Git Repo 自動部署。
**Harbor / SonarQube** 部署於另一台 DevOps Server。

---

## 3. 微服務清單

| 服務 | Port | 職責 | 資料庫 |
|---|---|---|---|
| `uaa-service` | 8081 | 使用者登入、JWT RS256 簽發、JWKS 端點 | PostgreSQL `uaa_db` |
| `iotcore-service` | 8082 | 站點/機台/IoT 元件設定、告警規則、Saga 協調、Outbox | PostgreSQL `iotcore_db` |
| `telemetry-service` | 8083 | Kafka 消費（Write path）→ TimescaleDB；REST 查詢（Read path） | TimescaleDB `telemetry_db` |
| `iot-adapter` | 8084 | EMQX MQTT 消費 → 正規化 → 發布到 Kafka | 無（無狀態） |
| `saas-bff` | 8090 | REST 聚合、RBAC 閘控、JWT 驗證、呼叫下游服務 | Redis（僅快取） |
| `realtime-bff` | 8091 | WebSocket 推播（儀表板即時資料）、Telegram 告警通知 | Redis（Session） |

---

## 4. Maven 多模組結構

```
willthx-parent/
├── pom.xml                          # 父 POM，管理版本與依賴
├── common-model/                    # 共用：DTO、Enum、Exception 基底、Snowflake ID
├── common-web/                      # 共用：GlobalExceptionHandler、Spring Security Config
├── uaa-service/
├── iotcore-service/
├── telemetry-service/
├── iot-adapter/
├── saas-bff/
└── realtime-bff/
```

### 套件根命名

```
com.willthx.{module}.*
```

範例：`com.willthx.iotcore.*`、`com.willthx.telemetry.*`、`com.willthx.saas.*`

---

## 5. 資料流架構

### 5.1 IoT 資料流（Telemetry）

```
Factory Device
    │ MQTT publish
    ▼
EMQX Broker
    │ 內部轉發
    ▼
iot-adapter（Spring Integration / MQTT Client）
    │ 正規化 Event Envelope
    ▼
Kafka（telemetry.{stationId}.{machineId}）
    ├──► telemetry-service（Write path → TimescaleDB）
    ├──► iotcore-service（告警規則評估）
    └──► realtime-bff（WebSocket 即時推播）
```

### 5.2 設備事件流（Device Events）

```
Factory Device
    │ MQTT publish（設備事件）
    ▼
EMQX → iot-adapter
    │ 封裝 Event Envelope
    ▼
Kafka（device-events.{stationId}）
    └──► iotcore-service（Lot/Unit 狀態機、Saga 協調）
              │ 業務告警觸發
              ▼
         RabbitMQ（alert.fanout Exchange）
              ├──► realtime-bff（WebSocket 告警推播）
              └──► realtime-bff（Telegram 通知）
```

### 5.3 前端請求流

```
Browser → Nginx → saas-bff（REST）
                      ├── uaa-service（身份驗證）
                      ├── iotcore-service（設定、規則、Lot/Unit 查詢）
                      └── telemetry-service（歷史遙測、圖表）

Browser → Nginx → realtime-bff（WebSocket）
                      ├── Kafka（即時 Telemetry）
                      └── RabbitMQ（即時告警）
```

---

## 6. MQTT Topic 命名規則

```
willthx/{tenantId}/{dataType}/{stationId}/{machineId}
```

| dataType | 說明 |
|---|---|
| `telemetry` | 感測器數值（溫度、壓力等） |
| `event` | 設備狀態事件（UNIT_LOADED、MACHINE_ERROR 等） |

---

## 7. Kafka Topic 命名規則

| Topic | 格式 | 說明 |
|---|---|---|
| Telemetry | `telemetry.{stationId}.{machineId}` | 感測器原始數據 |
| Device Events | `device-events.{stationId}` | 設備狀態事件（含 Lot/Unit 事件） |

Kafka 4.x 使用 **KRaft 模式**，不依賴 Zookeeper。

---

## 8. RabbitMQ 告警路由

```
Exchange: alert.fanout（Fanout 類型）
  ├── Queue: alert.websocket   → realtime-bff（WebSocket 推播）
  └── Queue: alert.telegram    → realtime-bff（Telegram Bot 發送）
```

RabbitMQ 負責業務層告警派發，不直接接收 IoT 原始資料。

---

## 9. 認證授權架構（UAA / JWT）

- `uaa-service` 自行實作，**不**使用 Keycloak 等外部 IdP
- JWT 使用 **RS256 非對稱簽名**（私鑰簽發、公鑰驗證）
- `uaa-service` 提供 `GET /internal/auth/jwks` 端點，供 BFF 取得公鑰
- BFF 在啟動時快取公鑰，並定期更新（或透過 Redis 快取）
- Refresh Token 儲存於 Redis，支援定向登出（JWT Blacklist by `jti`）
- 角色（Role）：`ADMIN`、`OPERATOR`、`VIEWER`

---

## 10. Saga 設計（iotcore-service）

採用 **Orchestration-based Saga**，由 `iotcore-service` 擔任協調器：

- **State Machine**：管理 Lot/Unit 狀態轉換
- **Outbox Pattern**：事件先寫入 `outbox` 資料表，由 `@Scheduled` Poller 發布（Demo 模式）
- **Idempotency**：`processed_events` 資料表防止重複消費 Kafka 訊息
- **Optimistic Lock**：Saga 狀態資料表加 `version` 欄位

---

## 11. K8s 資源規劃

| 服務 | Replicas | CPU Request | Memory Request |
|---|---|---|---|
| uaa-service | 1 | 100m | 256Mi |
| iotcore-service | 2 | 200m | 512Mi |
| telemetry-service | 2 | 200m | 512Mi |
| iot-adapter | 2 | 300m | 512Mi |
| saas-bff | 2 | 200m | 512Mi |
| realtime-bff | 2 | 200m | 512Mi |
| EMQX | 1 | 500m | 1Gi |
| Kafka | 3 | 500m | 1Gi |
| RabbitMQ | 1 | 200m | 512Mi |
| TimescaleDB | 1 | 500m | 2Gi |
| PostgreSQL | 1 | 300m | 1Gi |
| Redis | 1 | 100m | 256Mi |

---

## 12. 可觀測性

| 工具 | 職責 |
|---|---|
| **Prometheus** | 指標抓取（Spring Actuator `/actuator/prometheus`） |
| **Loki** | 日誌聚合（各服務 stdout → Promtail → Loki） |
| **Grafana** | 儀表板（Prometheus + Loki 資料源） |

每個服務需暴露：
- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

---

## 13. CI/CD 流程

```
Developer Push → GitHub
    │
    ▼
GitHub Actions（CI）
    ├── mvn test（JUnit + JaCoCo）
    ├── SonarQube 品質閘
    └── docker build + push to Harbor
    │
    ▼
ArgoCD（CD，GitOps）
    └── 偵測 Helm Chart 變更 → 自動部署到 k8s
```

---

## 14. 環境清單

| 環境 | 說明 |
|---|---|
| `local` | 開發者本機，使用 Docker Compose 啟動基礎設施 |
| `dev` | k8s 開發命名空間，ArgoCD 自動同步 |
| `prod` | k8s 生產命名空間，手動審核後合併觸發 |
