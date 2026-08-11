# 清潔架構規範（Clean Architecture）

> 適用範圍：所有 willThx 後端微服務（uaa-service、iotcore-service、telemetry-service、iot-adapter、saas-bff、realtime-bff）

---

## 1. Maven 多模組結構

```
willthx-parent/
├── pom.xml
├── common-model/          # 共用 DTO、Enum、Exception 基底、Snowflake ID 工具
├── common-web/            # 共用 GlobalExceptionHandler、Spring Security 設定
├── uaa-service/
│   ├── src/main/java/com/willthx/uaa/
│   │   ├── bootstrap/         # Spring Boot Application + 設定（Bean）
│   │   ├── domain/
│   │   │   ├── model/         # 領域模型（純 Java，無框架依賴）
│   │   │   ├── port/          # 輸入/輸出埠口介面
│   │   │   └── service/       # 領域服務實作
│   │   ├── application/
│   │   │   └── api/
│   │   │       ├── controller/ # REST Controller
│   │   │       └── dto/        # Rq / Rs DTO
│   │   └── adapter/
│   │       ├── persistence/    # JPA Entity、RepositoryImpl
│   │       ├── cache/          # Redis Adapter
│   │       └── messaging/      # Kafka / RabbitMQ Adapter（若有）
├── iotcore-service/       # 同上結構
├── telemetry-service/     # 同上結構
├── iot-adapter/           # MQTT → Kafka 橋接，adapter 為主體
├── saas-bff/              # BFF 含 UseCase 層
└── realtime-bff/          # WebSocket + Telegram 推播
```

---

## 2. 各層職責

### 2.1 Controller（`application/api/controller`）

- 負責 HTTP 入口：解析請求、呼叫 UseCase（BFF）或領域服務（業務服務）
- 回傳 `ResponseEntity<ApiResponse<T>>`，不包含業務邏輯
- 業務服務的 Controller 直接以 `@RequestHeader` 讀取 `X-User-Id`、`X-Tenant-Id`
- **不得** 直接呼叫 Repository 或任何 Adapter

### 2.2 UseCase（`application/usecase`，僅限 BFF）

- 每個業務流程對應一個 UseCase 介面與實作（`*UseCaseImpl`）
- 在 `execute(...)` 進入點**一次**呼叫 `UserContextHolder.get()`，之後以明確參數傳遞
- 負責跨服務呼叫的編排（Orchestration）邏輯
- **不得** 包含資料庫查詢或 HTTP 細節

### 2.3 Domain Service（`domain/service`）

- 核心業務邏輯所在：狀態機驗證、規則評估、資料計算
- 僅依賴 Port 介面，**不得** 直接依賴 JPA Repository 或 HTTP Client
- 所有方法以明確參數接收 `Long userId`（或 `TenantContext`），不讀取 ThreadLocal

### 2.4 Port（`domain/port`）

- 輸入埠（Input Port）：UseCase 介面（BFF 中）
- 輸出埠（Output Port）：Repository 介面、外部服務呼叫介面、訊息發布介面
- **純 Java 介面**，不含任何框架 annotation

### 2.5 Adapter（`adapter/`）

- 實作 Port 介面的具體類別
- `persistence/`：JPA RepositoryImpl，讀寫 PostgreSQL / TimescaleDB
- `cache/`：Redis Adapter（快取、JWT 黑名單）
- `messaging/`：Kafka Producer/Consumer、RabbitMQ Publisher/Listener、MQTT Client
- `client/`（BFF 專用）：Feign Client，呼叫下游業務服務

---

## 3. 命名規範

### 3.1 DTO 命名

| 類型 | 後綴 | 說明 |
|---|---|---|
| API 請求 | `Rq` | Controller 方法的 `@RequestBody` 參數 |
| API 回應 | `Rs` | Controller 方法回傳的資料物件 |
| 領域模型 | `Model` | 領域層流通的純 Java 物件 |
| JPA 實體 | `Entity` | 持久化層，不跨越 Adapter 邊界 |
| 值物件 | `Vo` | 不可變的領域值物件 |
| 跨服務傳輸 | `Dto` | Feign 回應解包後的傳輸物件 |
| Kafka 訊息 | `Event` | Kafka 訊息酬載（Event Envelope） |

### 3.2 類別命名範例

```
LoginRq            → Controller 接收的請求
LoginRs            → Controller 回傳的資料
UserModel          → Domain 層流通物件
UserEntity         → JPA 實體
TokenVo            → 不可變值物件（Access Token + Refresh Token）
AlertRuleDto       → Feign 解包的跨服務資料
TelemetryEvent     → Kafka 訊息物件
```

### 3.3 介面與實作

```
// Port（介面）
public interface AlertRuleRepository { ... }

// Adapter（實作）
public class AlertRuleRepositoryImpl implements AlertRuleRepository { ... }
```

UseCase 同理：
```
public interface CreateAlertRuleUseCase { ... }
public class CreateAlertRuleUseCaseImpl implements CreateAlertRuleUseCase { ... }
```

---

## 4. 依賴方向規則

```
Controller / UseCase
       │
       ▼
  Domain (Service + Port)
       │
       ▲（Port 介面，Adapter 實作）
  Adapter (Persistence / Cache / Messaging / Client)
```

- **domain 層不得依賴 adapter 層**（依賴反轉原則）
- common-model 可被所有模組依賴
- common-web 可被所有服務模組依賴
- iot-adapter 沒有傳統 Domain Service，以 Adapter 為主體（MQTT → Kafka 橋接）

---

## 5. 各服務特殊說明

### 5.1 iot-adapter

職責為 MQTT 消費 → 正規化 → Kafka 發布，結構簡化：

```
bootstrap/
adapter/
  mqtt/     # MQTT Message Listener（EMQX）
  kafka/    # Kafka Producer
  model/    # Event Envelope（正規化後的訊息格式）
```

不設 Domain Service 層，無 JPA，無 REST endpoint（除 Actuator）。

### 5.2 realtime-bff

職責為 WebSocket 推播 + Telegram 通知，結構：

```
bootstrap/
adapter/
  ws/         # WebSocket Handler（Spring WebSocket）
  kafka/      # Kafka Consumer（即時 Telemetry）
  rabbit/     # RabbitMQ Listener（告警）
  telegram/   # Telegram Bot API Client
application/
  api/
    ws/       # WebSocket Controller（@MessageMapping）
```

不設 UseCase 層，直接由 Listener 觸發推播。

### 5.3 telemetry-service

雙重職責（Write + Read）：

```
adapter/
  kafka/      # Kafka Consumer → 寫入 TimescaleDB（Write path）
  persistence/# TimescaleDB JdbcTemplate / JPA（Read + Write）
application/
  api/
    controller/ # REST 查詢端點（Read path，供 saas-bff 呼叫）
```

---

## 6. 禁止事項

| 禁止行為 | 說明 |
|---|---|
| Domain Service 直接依賴 JpaRepository | 必須透過 Port 介面 |
| Controller 包含業務邏輯 | 業務邏輯放 Domain Service |
| 跨服務直接呼叫（非 BFF 的服務互呼） | 業務服務間不直接 HTTP 呼叫，透過 Kafka/RabbitMQ 或由 BFF 聚合 |
| Entity 跨越 Adapter 邊界傳遞 | Entity 只在 Adapter 層內部使用，對外轉換為 Model |
| ThreadLocal 讀取在 Domain Service 或 Adapter | 僅 BFF JwtAuthFilter 設定，UseCase 進入點取一次後以參數傳遞 |
| 魔術字串取代 Enum 比較 | 使用 Enum 同一性（`==`） |

---

## 7. Spring Bean 宣告規範

- 優先使用建構子注入（Constructor Injection），搭配 `@RequiredArgsConstructor`
- 禁止使用 Field Injection（`@Autowired` 直接在欄位上）
- Configuration 類別集中在 `bootstrap/config/` 下

---

## 8. IoT 特有 Adapter 規範

### 8.1 Kafka Consumer（@KafkaListener）

```java
@KafkaListener(topics = "telemetry.${station.id}.#", groupId = "telemetry-writer")
public void onTelemetry(ConsumerRecord<String, String> record) {
    // 1. 反序列化 Event Envelope
    // 2. 冪等檢查（processed_events）
    // 3. 寫入 TimescaleDB
    // 4. 更新 processed_events
}
```

- 每個 Consumer 方法對應單一職責
- 冪等鍵（Idempotency Key）= `eventId`（來自 Event Envelope）

### 8.2 Outbox Poller（iotcore-service）

```java
@Scheduled(fixedDelay = 1000)
@Transactional
public void pollOutbox() {
    List<OutboxEntity> pending = outboxRepository.findPendingBatch(100);
    for (OutboxEntity entry : pending) {
        kafkaTemplate.send(entry.getTopic(), entry.getPayload());
        entry.markPublished();
    }
}
```

Demo 模式使用 `@Scheduled` Poller，生產模式可替換為 Debezium CDC。
