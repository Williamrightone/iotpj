# 資料管理規範（Database）

## 0. 資料庫環境

### 0.1 PostgreSQL（業務資料）

系統使用 **PostgreSQL 16** 作為業務資料庫，基於以下原因：

- 原生支援 JSONB（用於 `process_spec`、`alert_config` 等動態欄位）
- 完整的 CTE / 視窗函數支援
- 與 TimescaleDB 同為 PostgreSQL 生態，開發體驗一致
- 微服務架構下每個服務獨立資料庫

### 0.2 TimescaleDB（時序資料）

**TimescaleDB**（基於 PostgreSQL 16）作為獨立實例，用於儲存感測器遙測數據：

- Hypertable 自動分片（按時間）
- Continuous Aggregate（預聚合）提升查詢效能
- 資料保留策略（Data Retention Policy）自動清理舊資料

### 0.3 資料庫清單

| 服務 | 資料庫 | 類型 |
|---|---|---|
| uaa-service | `uaa_db` | PostgreSQL |
| iotcore-service | `iotcore_db` | PostgreSQL |
| telemetry-service | `telemetry_db` | TimescaleDB |

---

## 1. 主鍵策略

### 1.1 業務實體（PostgreSQL）— Snowflake ID

業務主鍵使用 **Snowflake ID**（64 位元 Long），具備：

- 全域唯一，避免跨服務 ID 碰撞
- 單調遞增，對 B-Tree 索引友善
- 完全在應用層生成，無資料庫自增瓶頸

**使用 Snowflake 的資料表：** `users`、`stations`、`machines`、`iot_components`、`alert_rules`、`alert_events`、`lots`、`units` 等主要業務實體。

**不需要 Snowflake 的資料表：** 設定種子資料表、關聯中間表（使用複合主鍵）。

```
common-model
└── id
      ├── SnowflakePodRegistry        # Redis Pod ID 租約 / 續約 / 釋放
      ├── SnowflakeIdGenerator        # ID 生成邏輯（DC+Pod+Seq）
      └── SnowflakeAutoConfiguration  # 自動設定
```

Pod ID 動態租約（Kubernetes）：

```
Pod 啟動 (@PostConstruct)
  -> 掃描 Redis，SETNX snowflake:pod:{dcId}:{id} EX 30
  -> 成功 -> 將 podId 存入實例變數

Pod 運行中 (@Scheduled，每 10 秒)
  -> EXPIRE 同 key，續約 TTL 30 秒

Pod 正常關閉 (@PreDestroy)
  -> DEL 對應 key

Pod 崩潰
  -> TTL 到期（30 秒），key 自動移除
```

### 1.2 IoT 事件（TimescaleDB）— UUID

`telemetry_records` 使用 **UUID**（`eventId`）作為冪等鍵，配合時間欄位作為 Hypertable 分片鍵：

```sql
-- 不設傳統 AUTO INCREMENT，以 (event_id, time) 作為主鍵
CREATE TABLE telemetry_records (
    event_id    UUID          NOT NULL,
    time        TIMESTAMPTZ   NOT NULL,
    tenant_id   BIGINT        NOT NULL,
    station_id  VARCHAR(64)   NOT NULL,
    machine_id  VARCHAR(64)   NOT NULL,
    metric_name VARCHAR(64)   NOT NULL,
    value       DOUBLE PRECISION NOT NULL,
    unit        VARCHAR(16),
    PRIMARY KEY (event_id, time)
);

SELECT create_hypertable('telemetry_records', 'time');
```

---

## 2. 外鍵策略

PostgreSQL 不在資料庫層級強制跨服務 FK。參照完整性在應用層驗證：

- 微服務架構下，每個服務擁有自己的 PostgreSQL database，資料庫層級 FK 無法跨越 database 邊界
- 跨服務的資料一致性由 Saga Pattern（iotcore-service）管理
- 同一資料庫內的表間 FK 約束**可以**使用，但不強制（避免高寫入場景的鎖定開銷）

---

## 3. JSONB 欄位使用規範

PostgreSQL JSONB 用於結構彈性的欄位：

| 資料表 | 欄位 | 說明 |
|---|---|---|
| `products` | `process_spec` | 各站點製程規格（閾值設定） |
| `alert_rules` | `condition` | 告警條件（操作符、閾值） |
| `unit_station_records` | `metrics_snapshot` | 站點通過時的感測器快照 |

JSONB 欄位命名使用 `snake_case`，JPA 以 `@JdbcTypeCode(SqlTypes.JSON)` 映射：

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "process_spec", columnDefinition = "jsonb")
private Map<String, Object> processSpec;
```

---

## 4. JPA 與原生查詢

| 場景 | 方式 |
|---|---|
| 簡單 CRUD | Spring Data JPA（`JpaRepository`） |
| 含 JOIN 的複雜查詢 | JPQL 或 `@Query(nativeQuery=true)` |
| TimescaleDB 時序聚合查詢 | `JdbcTemplate`（原生 SQL） |
| Outbox Batch Poll | `@Query(nativeQuery=true)` + `@Lock` |

---

## 5. Flyway 資料庫版本管理

### 5.1 版本號規則

版本格式：`{major}.{minor}.{patch}`，從 `1.0.0` 開始。

| 情況 | 規則 |
|---|---|
| 一般變更 | patch + 1（`1.0.0` → `1.0.1`） |
| 跨模組或發布邊界 | minor + 1（`1.0.x` → `1.1.0`） |

### 5.2 檔案命名規則

- DDL 變更（建表、修改欄位）：`create_` 或 `alter_` 開頭
- DML 變更（種子資料）：`insert_` 開頭
- TimescaleDB 特有操作（Hypertable、Continuous Aggregate）：`setup_` 開頭

```
V1.0.0__create_table_stations.sql
V1.0.1__create_table_machines.sql
V1.0.2__create_table_iot_components.sql
V1.0.3__create_table_alert_rules.sql
V1.1.0__create_table_lots.sql
V1.1.1__create_table_units.sql
V1.1.2__create_table_unit_station_records.sql
V1.2.0__create_table_outbox.sql
V1.2.1__create_table_processed_events.sql
V1.2.2__insert_seed_products.sql

-- telemetry_db（TimescaleDB）
V1.0.0__create_table_telemetry_records.sql
V1.0.1__setup_hypertable_telemetry_records.sql
V1.0.2__setup_continuous_aggregate_telemetry_hourly.sql
```

### 5.3 Spring 設定

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  datasource:
    url: jdbc:postgresql://postgres:5432/iotcore_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## 6. BaseTimeEntity

所有 JPA 實體必須繼承 `BaseTimeEntity`：

```java
@MappedSuperclass
@Getter
@Setter
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

**規則：**

- `BaseTimeEntity` 定義在 `common-model`，所有服務的實體必須繼承
- 實體不得手動設定 `createdAt` / `updatedAt`，由 JPA 生命週期回呼獨佔管理
- TimescaleDB 的 `telemetry_records` 使用 `TIMESTAMPTZ time` 欄位，不繼承 `BaseTimeEntity`

---

## 7. 樂觀鎖（Optimistic Lock）

需要並行控制的資料表必須加 `version` 欄位：

適用資料表：`lot_sagas`、`unit_sagas`（Saga 狀態機）、`alert_rules`（設定並發更新）

```sql
ALTER TABLE lot_sagas ADD COLUMN version INT NOT NULL DEFAULT 0;
```

```java
@Version
@Column(name = "version", nullable = false)
private Integer version;
```

JPA 自動在 UPDATE 中附加 `AND version = ?`，版本不符則拋出 `OptimisticLockException`。

---

## 8. Enum 持久化

JPA 實體中所有 Enum 欄位使用 `@Enumerated(EnumType.STRING)`：

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 30)
private LotStatus status;
```

**規則：**

- 不得使用 `@Enumerated(EnumType.ORDINAL)`（列舉順序變更時序數值無聲中斷）
- 資料庫欄位型別為 `VARCHAR`，長度匹配最長的列舉名稱（加緩衝）
- Flyway 種子資料使用確切的列舉名稱字串（例如 `'ACTIVE'`，而非 `0`）

---

## 9. 多租戶資料隔離

willThx 為 SaaS 平台，採用**共用資料庫 + `tenant_id` 欄位**的多租戶模式：

- 所有業務資料表必須包含 `tenant_id BIGINT NOT NULL` 欄位
- 所有查詢必須帶入 `tenant_id` 條件（在 Repository 層強制）
- 禁止跨租戶資料存取（由 Domain Service 驗證）

```java
// 正確：查詢帶 tenantId
Optional<LotEntity> findByIdAndTenantId(Long id, Long tenantId);

// 禁止：不帶 tenantId 的全資料查詢
Optional<LotEntity> findById(Long id);  // 不允許在 Repository Port 暴露此方法
```

---

## 10. TimescaleDB 特有設定

### Hypertable

```sql
-- 建立 Hypertable（按 time 欄位分片，chunk 間隔 7 天）
SELECT create_hypertable('telemetry_records', 'time',
    chunk_time_interval => INTERVAL '7 days');
```

### Continuous Aggregate（預聚合）

```sql
-- 每小時聚合（供儀表板查詢用）
CREATE MATERIALIZED VIEW telemetry_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    tenant_id, station_id, machine_id, metric_name,
    AVG(value) AS avg_value,
    MAX(value) AS max_value,
    MIN(value) AS min_value
FROM telemetry_records
GROUP BY bucket, tenant_id, station_id, machine_id, metric_name;

-- 自動重新整理策略
SELECT add_continuous_aggregate_policy('telemetry_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset   => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');
```

### 資料保留策略

```sql
-- 原始資料保留 90 天
SELECT add_retention_policy('telemetry_records', INTERVAL '90 days');
-- 聚合資料保留 2 年
SELECT add_retention_policy('telemetry_hourly', INTERVAL '730 days');
```
