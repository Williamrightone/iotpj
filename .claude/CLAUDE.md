# willThx IoT Platform

PCB 製造業 SaaS IoT 平台。回應使用繁體中文。

---

## 技術棧速覽

Java 21 + Spring Boot 3.x | Maven 多模組 | PostgreSQL 16 | TimescaleDB | Redis 7
Kafka 3.9 (KRaft) | RabbitMQ | EMQX | Kubernetes | 前端：純 HTML/CSS/JS

---

## 模組結構

```
backend/
├── common-model/       com.willthx.common.model.*   # DTO、Enum、Exception 基底、Snowflake ID
├── common-web/         com.willthx.common.web.*     # GlobalExceptionHandler
├── uaa-service/        com.willthx.uaa.*            # JWT RS256 :8081
├── iotcore-service/    com.willthx.iotcore.*        # Saga + 告警 + IoT 設定 :8082
├── telemetry-service/  com.willthx.telemetry.*      # Kafka→TimescaleDB + REST :8083
├── iot-adapter/        com.willthx.adapter.*        # MQTT→Kafka :8084
├── saas-bff/           com.willthx.saas.*           # REST 聚合 BFF :8090
└── realtime-bff/       com.willthx.realtime.*       # WebSocket + Telegram :8091
```

---

## 分層架構（所有服務）

```
bootstrap/ → application/(controller/usecase) → domain/(service/port) ← adapter/
```

- domain 不得依賴 adapter（依賴反轉）
- BFF 才有 usecase 層；iot-adapter 無 domain service
- 詳見 `doc/rules/clean-architecture.md`

---

## 命名後綴（快查）

`Rq` 請求 | `Rs` 回應 | `Model` 領域物件 | `Entity` JPA | `Vo` 值物件 | `Dto` 跨服務 | `Event` Kafka 訊息

---

## 硬規則（每次都適用）

- **只用建構子注入**，禁 `@Autowired` Field Injection
- **所有業務資料表必須有 `tenant_id`**，查詢必須帶 tenantId 條件
- **`UserContextHolder` 只在 saas-bff**，業務服務不得使用
- **Enum 持久化**只用 `@Enumerated(EnumType.STRING)`
- **所有 JPA 實體繼承 `BaseTimeEntity`**

---

## 文件索引

| 主題 | 文件 |
|---|---|
| 架構拓樸、資料流、K8s 規劃 | `doc/architecture.md` |
| 分層規範、各服務結構說明 | `doc/rules/clean-architecture.md` |
| ApiResponse 封裝與使用規則 | `doc/rules/api-response.md` |
| JWT 驗證、UserContext、RBAC、內部標頭 | `doc/rules/auth-context.md` |
| 例外類別、錯誤碼格式、Feign 例外處理 | `doc/rules/exception.md` |
| 測試策略、JaCoCo 設定、Testcontainers | `doc/rules/testing.md` |
| 主鍵策略、Flyway、多租戶、TimescaleDB | `doc/rules/database.md` |
| PRD 情境與業務流程 | `domain/prd/1-process-scenario.md` |
| PRD 架構設計（含 Saga 詳細設計） | `domain/prd/3-architecture.md` |
| PRD 資料設計 | `domain/prd/4-data-design.md` |
| 前端原型 | `prototype/` |
