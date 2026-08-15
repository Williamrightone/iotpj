# SPEC-4：製程配置管理

> 對應 PRD：PRD-006
> 版本：v0.1
> 日期：2026-08-14
> 服務：iotcore-service (:8082)、saas-bff (:8090)
> 狀態：草稿

---

## 1. 領域模型

### Entity（iotcore-service / iotcore_db）

**stations**

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT NOT NULL | 多租戶隔離 |
| `station_code` | VARCHAR(64) NOT NULL | 使用者定義，`UNIQUE(tenant_id, station_code)`，建立後不可修改 |
| `name` | VARCHAR(100) NOT NULL | 顯示名稱，可修改 |
| `description` | VARCHAR(255) | 說明，選填，可修改 |
| `sort_order` | INT NOT NULL DEFAULT 0 | 製程順序，拖拉排序時更新 |
| `is_active` | TINYINT(1) NOT NULL DEFAULT 1 | 軟刪除旗標 |
| `created_at` | DATETIME(3) NOT NULL | BaseTimeEntity |
| `updated_at` | DATETIME(3) NOT NULL | BaseTimeEntity |

**machines**

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT NOT NULL | |
| `station_id` | BIGINT NOT NULL | FK stations.id（無 DB-level cascade） |
| `machine_code` | VARCHAR(64) NOT NULL | 使用者定義，`UNIQUE(tenant_id, machine_code)`，建立後不可修改 |
| `name` | VARCHAR(100) NOT NULL | 顯示名稱，可修改 |
| `model` | VARCHAR(100) | 型號備註，選填，可修改 |
| `is_active` | TINYINT(1) NOT NULL DEFAULT 1 | |
| `created_at` | DATETIME(3) NOT NULL | |
| `updated_at` | DATETIME(3) NOT NULL | |

**iot_components**

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT NOT NULL | |
| `station_id` | BIGINT NOT NULL | FK stations.id |
| `machine_id` | BIGINT | FK machines.id；NULL = 站點層級元件 |
| `component_code` | VARCHAR(64) NOT NULL | 使用者定義，建立後不可修改 |
| — | UNIQUE(tenant_id, machine_id, component_code) | machine_id IS NULL 時 UNIQUE(tenant_id, station_id, component_code) 由應用層保證 |
| `name` | VARCHAR(100) NOT NULL | 顯示名稱，可修改 |
| `data_type` | VARCHAR(20) NOT NULL | `TELEMETRY` / `EVENT`，`@Enumerated(STRING)` |
| `unit` | VARCHAR(30) | 數值單位，TELEMETRY 必填 |
| `report_interval_sec` | INT | 上報頻率秒數；0 = 事件驅動；TELEMETRY 必填 |
| `normal_upper` | DECIMAL(15,4) | 正常上限，選填 |
| `normal_lower` | DECIMAL(15,4) | 正常下限，選填 |
| `is_active` | TINYINT(1) NOT NULL DEFAULT 1 | |
| `created_at` | DATETIME(3) NOT NULL | |
| `updated_at` | DATETIME(3) NOT NULL | |

### Enum（common-model 新增）

```java
public enum ComponentDataType { TELEMETRY, EVENT }
```

---

## 2. 錯誤碼

> 前綴 `IC`（iotcore-service），`SB` 為 saas-bff 層（已存在於 SPEC-1 ~3）

| 錯誤碼 | 來源 | 說明 |
|--------|------|------|
| IC00001 | 本 spec 新增 | 站點不存在或不屬於當前租戶 |
| IC00002 | 本 spec 新增 | 機台不存在或不屬於當前租戶 |
| IC00003 | 本 spec 新增 | IoT 元件不存在或不屬於當前租戶 |
| IC00004 | 本 spec 新增 | 站點代碼已存在（stationCode 在租戶內重複） |
| IC00005 | 本 spec 新增 | 機台代碼已存在（machineCode 在租戶內重複） |
| IC00006 | 本 spec 新增 | 元件代碼已存在（componentCode 在同機台/同站點層級內重複） |
| IC00007 | 本 spec 新增 | 代碼欄位不可修改（stationCode / machineCode / componentCode） |
| SB00001 | 已存在 SPEC-1 | 無操作權限（Viewer 嘗試寫入） |

> Viewer 嘗試任何寫入操作時，saas-bff UseCase 層擲出 SB00001，回傳 HTTP 403。
> Admin 與 Maintainer 擁有相同寫入權限（PRD-006 §2）。

---

## 3. API 規格

> 所有端點回傳 `ResponseEntity<ApiResponse<T>>`
> 所有端點須通過 JwtAuthFilter
> 寫入操作：role == VIEWER → SB00001

### saas-bff (:8090)

#### GET /api/stations

- **說明**：查詢本租戶所有站點（含各站啟用機台數）
- **RBAC**：Admin / Maintainer / Viewer
- **Response**：`ApiResponse<List<StationRs>>`
  ```json
  [{
    "id":           1234567890,
    "stationCode":  "S01",
    "name":         "錫膏印刷",
    "description":  null,
    "sortOrder":    1,
    "isActive":     true,
    "activeMachineCount": 10,
    "activeComponentCount": 0
  }]
  ```

#### POST /api/stations

- **說明**：新增站點
- **RBAC**：Admin / Maintainer
- **Request Body**：
  ```json
  {
    "stationCode": "S06",
    "name":        "表面處理",
    "description": "選填說明"
  }
  ```
- **Response**：`ApiResponse<StationRs>`
- **錯誤情境**：IC00004（stationCode 重複）

#### PUT /api/stations/{id}

- **說明**：更新站點資訊（stationCode 不可修改）
- **RBAC**：Admin / Maintainer
- **路徑參數**：`id`（stationId）
- **Request Body**：
  ```json
  {
    "name":        "表面處理（更新）",
    "description": "說明"
  }
  ```
- **Response**：`ApiResponse<StationRs>`
- **錯誤情境**：IC00001

#### POST /api/stations/reorder

- **說明**：批次更新站點排序（拖拉完成後呼叫）
- **RBAC**：Admin / Maintainer
- **Request Body**：
  ```json
  { "orders": [{ "id": 1, "sortOrder": 1 }, { "id": 2, "sortOrder": 2 }] }
  ```
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：IC00001（任一 id 不存在）

#### POST /api/stations/{id}/deactivate

- **說明**：停用站點，**連帶停用**所有啟用中的機台及元件（含站點層級元件）
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<DeactivateResultRs>`
  ```json
  { "deactivatedMachines": 10, "deactivatedComponents": 33 }
  ```
- **錯誤情境**：IC00001

#### POST /api/stations/{id}/activate

- **說明**：重新啟用站點（**不**連帶啟用底層機台/元件）
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<StationRs>`
- **錯誤情境**：IC00001

#### GET /api/stations/{id}/detail

- **說明**：查詢站點詳情（含站點層級元件、機台列表及各機台元件）
- **RBAC**：Admin / Maintainer / Viewer
- **Response**：`ApiResponse<StationDetailRs>`
  ```json
  {
    "station": { "id": 1, "stationCode": "S01", "name": "錫膏印刷", ... },
    "stationComponents": [
      { "id": 101, "componentCode": "ENV-TEMP", "name": "廠區溫度", "dataType": "TELEMETRY",
        "unit": "°C", "reportIntervalSec": 30, "normalUpper": 26.0, "normalLower": 20.0,
        "isActive": true }
    ],
    "machines": [
      {
        "id": 100, "machineCode": "S01-M01", "name": "印刷機-01", "model": "DEK Horizon 03i",
        "isActive": true,
        "components": [
          { "id": 200, "componentCode": "SP-PRESSURE", "name": "刮刀壓力", "dataType": "TELEMETRY",
            "unit": "kgf", "reportIntervalSec": 1, "normalUpper": 14.0, "normalLower": 6.0,
            "isActive": true }
        ]
      }
    ]
  }
  ```

---

#### POST /api/stations/{stationId}/machines

- **說明**：在指定站點下新增機台
- **RBAC**：Admin / Maintainer
- **Request Body**：
  ```json
  {
    "machineCode": "S01-M11",
    "name":        "印刷機-11",
    "model":       "DEK Horizon 03i"
  }
  ```
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00001、IC00005（machineCode 重複）

#### PUT /api/machines/{id}

- **說明**：更新機台資訊（machineCode 不可修改）
- **RBAC**：Admin / Maintainer
- **Request Body**：`{ "name": "印刷機-11 更新", "model": "DEK NeoHorizon" }`
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00002

#### POST /api/machines/{id}/deactivate

- **說明**：停用機台，**連帶停用**該機台下所有啟用中的元件
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<DeactivateResultRs>`
  ```json
  { "deactivatedMachines": 1, "deactivatedComponents": 3 }
  ```
- **錯誤情境**：IC00002

#### POST /api/machines/{id}/activate

- **說明**：重新啟用機台（不連帶啟用底層元件）
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00002

#### POST /api/machines/{id}/copy

- **說明**：複製機台（包含所有啟用中的元件設定）
- **RBAC**：Admin / Maintainer
- **Request Body**：
  ```json
  {
    "newMachineCode": "S01-M12",
    "newName":        "印刷機-12"
  }
  ```
- **Response**：`ApiResponse<MachineRs>`（新建立的機台）
- **錯誤情境**：IC00002（來源不存在）、IC00005（新 machineCode 重複）

---

#### POST /api/stations/{stationId}/components

- **說明**：新增站點層級元件（machineId = NULL，如廠務環境感測器）
- **RBAC**：Admin / Maintainer
- **Request Body**：（同機台元件，見下）
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00001、IC00006（componentCode 在同站點層級重複）

#### POST /api/machines/{machineId}/components

- **說明**：新增機台元件
- **RBAC**：Admin / Maintainer
- **Request Body**：
  ```json
  {
    "componentCode":    "SP-PRESSURE",
    "name":             "刮刀壓力",
    "dataType":         "TELEMETRY",
    "unit":             "kgf",
    "reportIntervalSec": 1,
    "normalUpper":      14.0,
    "normalLower":      6.0
  }
  ```
  > EVENT 型別：unit、reportIntervalSec、normalUpper、normalLower 均可省略
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00002、IC00006（componentCode 在同機台重複）

#### PUT /api/components/{id}

- **說明**：更新元件資訊（componentCode 不可修改）
- **RBAC**：Admin / Maintainer
- **Request Body**：`{ "name": "更新名稱", "unit": "Pa", "reportIntervalSec": 2, "normalUpper": 15.0, "normalLower": 5.0 }`
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00003

#### POST /api/components/{id}/deactivate

- **說明**：停用元件（單獨停用，無連帶效果）
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：IC00003

#### POST /api/components/{id}/activate

- **說明**：重新啟用元件
- **RBAC**：Admin / Maintainer
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00003

---

### iotcore-service (:8082)

> 所有 internal API 不需 JWT，由 saas-bff 注入 `X-Tenant-Id` header
> RBAC 在 saas-bff 層已完成，iotcore 只驗 tenant 隔離

#### GET /internal/stations

- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<List<StationSummaryRs>>`（含 activeMachineCount、activeComponentCount）

#### POST /internal/stations

- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：`{ "stationCode": "S06", "name": "表面處理", "description": null }`
- **Response**：`ApiResponse<StationRs>`
- **錯誤情境**：IC00004

#### PUT /internal/stations/{id}

- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：`{ "name": "...", "description": "..." }`
- **Response**：`ApiResponse<StationRs>`
- **錯誤情境**：IC00001

#### POST /internal/stations/reorder

- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：`{ "orders": [{ "id": 1, "sortOrder": 1 }] }`
- **Response**：`ApiResponse<Void>`

#### POST /internal/stations/{id}/deactivate

- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<DeactivateResultRs>`

#### POST /internal/stations/{id}/activate

- **Response**：`ApiResponse<StationRs>`

#### GET /internal/stations/{id}/detail

- **Response**：`ApiResponse<StationDetailRs>`（含 machines + components）

#### POST /internal/stations/{stationId}/machines

- **Request Body**：`{ "machineCode": "S01-M11", "name": "...", "model": "..." }`
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00001、IC00005

#### PUT /internal/machines/{id}

- **Request Body**：`{ "name": "...", "model": "..." }`
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00002

#### POST /internal/machines/{id}/deactivate

- **Response**：`ApiResponse<DeactivateResultRs>`
- **錯誤情境**：IC00002

#### POST /internal/machines/{id}/activate

- **Response**：`ApiResponse<MachineRs>`

#### POST /internal/machines/{id}/copy

- **Request Body**：`{ "newMachineCode": "S01-M12", "newName": "印刷機-12" }`
- **Response**：`ApiResponse<MachineRs>`
- **錯誤情境**：IC00002、IC00005

#### POST /internal/stations/{stationId}/components

- **Request Body**：（同機台元件欄位，machineId 固定為 null）
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00001、IC00006

#### POST /internal/machines/{machineId}/components

- **Request Body**：`{ "componentCode": "SP-PRESSURE", "name": "刮刀壓力", "dataType": "TELEMETRY", "unit": "kgf", "reportIntervalSec": 1, "normalUpper": 14.0, "normalLower": 6.0 }`
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00002、IC00006

#### PUT /internal/components/{id}

- **Request Body**：`{ "name": "...", "unit": "...", "reportIntervalSec": 2, "normalUpper": 15.0, "normalLower": 5.0 }`
- **Response**：`ApiResponse<ComponentRs>`
- **錯誤情境**：IC00003

#### POST /internal/components/{id}/deactivate

- **Response**：`ApiResponse<Void>`

#### POST /internal/components/{id}/activate

- **Response**：`ApiResponse<ComponentRs>`

---

## 4. 業務邏輯

### 4.1 新增站點

```
POST /api/stations (saas-bff)
  UseCase:
    ctx = UserContextHolder.get()
    role == VIEWER → throw SB00001

  呼叫 POST /internal/stations (iotcore)
    @Transactional:
      a. 查 stations WHERE tenant_id=? AND station_code=? → 存在 → IC00004
      b. sortOrder = MAX(sort_order) + 1（當前租戶下）
      c. INSERT stations（id=Snowflake, tenant_id, station_code, name, description, sort_order, is_active=1）
```

### 4.2 停用站點（連帶停用）

```
POST /api/stations/{id}/deactivate (saas-bff)
  UseCase:
    role == VIEWER → throw SB00001

  呼叫 POST /internal/stations/{id}/deactivate (iotcore)
    @Transactional:
      a. 查 stations WHERE id=? AND tenant_id=? → 不存在 → IC00001
      b. UPDATE stations SET is_active=0 WHERE id=?
      c. 取得所有啟用中機台 ids：SELECT id FROM machines WHERE station_id=? AND is_active=1
      d. UPDATE machines SET is_active=0 WHERE station_id=? AND is_active=1
      e. UPDATE iot_components SET is_active=0 WHERE (machine_id IN (:machineIds) OR (station_id=? AND machine_id IS NULL)) AND is_active=1
      f. 回傳 deactivatedMachines 數、deactivatedComponents 數
```

### 4.3 重新啟用站點（不連帶）

```
POST /api/stations/{id}/activate (iotcore)
  @Transactional:
    a. 查 stations WHERE id=? AND tenant_id=? → 不存在 → IC00001
    b. UPDATE stations SET is_active=1 WHERE id=?
    c. 不更動底層 machines / iot_components
```

### 4.4 停用機台（連帶停用元件）

```
POST /internal/machines/{id}/deactivate (iotcore)
  @Transactional:
    a. 查 machines WHERE id=? AND tenant_id=? → 不存在 → IC00002
    b. UPDATE machines SET is_active=0 WHERE id=?
    c. UPDATE iot_components SET is_active=0 WHERE machine_id=? AND is_active=1
    d. 回傳 deactivatedComponents 數
```

### 4.5 複製機台

```
POST /internal/machines/{id}/copy (iotcore)
  @Transactional:
    a. 查 machines WHERE id=? AND tenant_id=? → 不存在 → IC00002
    b. 查 machines WHERE tenant_id=? AND machine_code=newMachineCode → 存在 → IC00005
    c. INSERT machines（id=Snowflake, tenant_id, station_id=source.stationId, machine_code=newMachineCode, name=newName, model=source.model）
    d. 查 iot_components WHERE machine_id=sourceId AND is_active=1（只複製啟用中的）
    e. 對每個元件：INSERT iot_components（新 id, 同 componentCode/name/dataType/unit/reportIntervalSec/normalUpper/normalLower, machine_id=新機台 id, is_active=1）
    f. 回傳新建立的 MachineRs
```

### 4.6 新增元件（代碼唯一性驗證）

```
POST /internal/machines/{machineId}/components (iotcore)
  @Transactional:
    a. 查 machines WHERE id=? AND tenant_id=? → 不存在 → IC00002
    b. 查 iot_components WHERE machine_id=? AND component_code=? → 存在 → IC00006
    c. TELEMETRY 且 unit 為空 → IC validation error（HTTP 400）
    d. INSERT iot_components

POST /internal/stations/{stationId}/components (iotcore)  [站點層級]
  @Transactional:
    a. 查 stations WHERE id=? AND tenant_id=? → 不存在 → IC00001
    b. 查 iot_components WHERE station_id=? AND machine_id IS NULL AND component_code=? → 存在 → IC00006
    c. INSERT iot_components（machine_id=NULL）
```

### 4.7 更新資訊（代碼不可修改）

> PUT /internal/stations/{id}、PUT /internal/machines/{id}、PUT /internal/components/{id}
>
> 所有 PUT 端點的 Request Body 中不包含 code 欄位（stationCode / machineCode / componentCode）。
> 若前端錯誤傳入 code，直接忽略，不做更新，不拋出錯誤。

### 4.8 排序更新

```
POST /internal/stations/reorder
  @Transactional:
    對 orders 列表中每一筆：
      UPDATE stations SET sort_order=? WHERE id=? AND tenant_id=?
      （不存在則忽略，不拋出錯誤）
```

---

## 5. Response DTO（iotcore-service 定義，saas-bff 透傳）

**StationRs**
```
{ id, stationCode, name, description, sortOrder, isActive, createdAt, updatedAt }
```

**StationSummaryRs**（列表頁用）
```
{ id, stationCode, name, description, sortOrder, isActive, activeMachineCount, activeComponentCount }
```

**StationDetailRs**（詳情頁用）
```
{ station: StationRs, stationComponents: List<ComponentRs>, machines: List<MachineDetailRs> }
```

**MachineRs**
```
{ id, stationId, machineCode, name, model, isActive, createdAt, updatedAt }
```

**MachineDetailRs**
```
{ id, stationId, machineCode, name, model, isActive, components: List<ComponentRs> }
```

**ComponentRs**
```
{ id, stationId, machineId, componentCode, name, dataType, unit, reportIntervalSec, normalUpper, normalLower, isActive, createdAt, updatedAt }
```

**DeactivateResultRs**
```
{ deactivatedMachines: int, deactivatedComponents: int }
```

---

## 6. DB Migration（iotcore-service）

Flyway 檔案路徑：`backend/iotcore-service/src/main/resources/db/migration/`

**V1__create_process_config_tables.sql**

```sql
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
  id           BIGINT       NOT NULL PRIMARY KEY,
  tenant_id    BIGINT       NOT NULL,
  station_id   BIGINT       NOT NULL COMMENT 'FK stations.id',
  machine_code VARCHAR(64)  NOT NULL COMMENT '機台代碼（不可修改）',
  name         VARCHAR(100) NOT NULL,
  model        VARCHAR(100)         COMMENT '型號備註',
  is_active    TINYINT(1)   NOT NULL DEFAULT 1,
  created_at   DATETIME(3)  NOT NULL,
  updated_at   DATETIME(3)  NOT NULL,
  UNIQUE KEY uq_machine_code (tenant_id, machine_code),
  INDEX idx_machine_station (station_id),
  INDEX idx_machine_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE iot_components (
  id                  BIGINT        NOT NULL PRIMARY KEY,
  tenant_id           BIGINT        NOT NULL,
  station_id          BIGINT        NOT NULL COMMENT 'FK stations.id',
  machine_id          BIGINT                 COMMENT 'FK machines.id；NULL = 站點層級',
  component_code      VARCHAR(64)   NOT NULL COMMENT '元件代碼（不可修改）',
  name                VARCHAR(100)  NOT NULL,
  data_type           VARCHAR(20)   NOT NULL COMMENT 'TELEMETRY / EVENT',
  unit                VARCHAR(30)            COMMENT 'TELEMETRY 必填',
  report_interval_sec INT                    COMMENT '上報頻率秒數；0 = 事件驅動',
  normal_upper        DECIMAL(15,4)          COMMENT '正常上限',
  normal_lower        DECIMAL(15,4)          COMMENT '正常下限',
  is_active           TINYINT(1)    NOT NULL DEFAULT 1,
  created_at          DATETIME(3)   NOT NULL,
  updated_at          DATETIME(3)   NOT NULL,
  INDEX idx_comp_machine (machine_id),
  INDEX idx_comp_station (station_id),
  INDEX idx_comp_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> machine_id 為 NULL 時的 componentCode 唯一性（同站點層級）由應用層 §4.6 保證，不依賴 DB unique index。

---

## 7. 套件依賴說明

iotcore-service 新增 module（backend/pom.xml 中尚未建立），需要：
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `mysql-connector-j`
- `flyway-core` + `flyway-mysql`
- `common-model`（Snowflake ID、ApiResponse、例外基底）
- `common-web`（GlobalExceptionHandler）
- Feign（供 saas-bff 呼叫 iotcore internal API 使用）
