# SPEC-2：使用者管理

> 對應 PRD：PRD-005
> 版本：v0.2
> 日期：2026-08-13
> 服務：uaa-service (:8081)、saas-bff (:8090)
> 狀態：草稿

---

## 1. 領域模型

### Enum

```java
// common-model（已存在）
public enum Role { ADMIN, MAINTAINER, VIEWER }

// uaa-service 新增
public enum UserStatus { ACTIVE, DISABLED }
```

### Entity

**users**

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT | 所屬租戶 |
| `account` | VARCHAR(100) NOT NULL | Email，`UNIQUE(account)` |
| `display_name` | VARCHAR(100) NOT NULL | 顯示姓名 |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt |
| `role` | VARCHAR(30) NOT NULL | `@Enumerated(STRING)` |
| `status` | VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' | `@Enumerated(STRING)` |
| `last_login_at` | TIMESTAMP | |
| `created_at` | TIMESTAMP NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP NOT NULL | BaseTimeEntity |

**user_station_bindings**（Maintainer / Viewer 的站點範圍）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `user_id` | BIGINT NOT NULL | |
| `station_id` | VARCHAR(64) NOT NULL | 站點代碼字串（跨服務，不做 DB FK） |
| `tenant_id` | BIGINT NOT NULL | |
| — | UNIQUE(user_id, station_id) | |

> Admin 此表永遠無資料。未設定任何站點的 Maintainer / Viewer 視為可存取全部站點。

### Redis 異動

| 操作 | Redis 影響 |
|------|-----------|
| 停用帳號 | 無（純 DB 操作；現有 access token 至多 15 分鐘後自然過期，refresh 時 uaa 檢查 status 拒絕） |
| 更新站點綁定 | DEL `user:stations:{userId}`（讓 JwtAuthFilter 下次重新從 DB 載入新綁定）|

---

## 2. API 規格

> 所有端點回傳 `ResponseEntity<ApiResponse<T>>`
> 所有端點須通過 JwtAuthFilter；RBAC 在 saas-bff UseCase 層檢查（role != ADMIN → SB00001）

### saas-bff (:8090)

#### GET /api/users

- **說明**：查詢本租戶所有使用者清單
- **RBAC**：Admin only
- **Request Header**：`Authorization: Bearer {accessToken}`
- **Response**：`ApiResponse<List<UserRs>>`
  ```json
  [{
    "userId":      1234567890,
    "account":     "john@factory.com",
    "displayName": "John",
    "role":        "MAINTAINER",
    "status":      "ACTIVE",
    "stationIds":  ["S01", "S02"],
    "lastLoginAt": "2026-08-12T09:00:00",
    "createdAt":   "2026-08-01T00:00:00"
  }]
  ```

#### POST /api/users

- **說明**：建立新使用者帳號
- **RBAC**：Admin only
- **Request Body**：
  ```json
  {
    "account":     "jane@factory.com",
    "displayName": "Jane",
    "password":    "初始密碼（明文，HTTPS 傳輸）",
    "role":        "VIEWER",
    "stationIds":  ["S01"]
  }
  ```
  > `stationIds` 為空陣列 = 可存取全部站點
- **Response**：`ApiResponse<UserRs>`（同 GET 單筆格式）
- **錯誤情境**：UA00007（account 重複）

#### PUT /api/users/{id}

- **說明**：更新使用者資訊（account 不可修改）
- **RBAC**：Admin only
- **路徑參數**：`id`（userId）
- **Request Body**：
  ```json
  {
    "displayName": "Jane Wu",
    "role":        "MAINTAINER",
    "stationIds":  []
  }
  ```
- **Response**：`ApiResponse<UserRs>`
- **錯誤情境**：UA00001（使用者不存在）、SB00003（修改自己的 role）

#### POST /api/users/{id}/disable

- **說明**：停用帳號（軟刪除）；現有 session 至多 15 分鐘後自然失效
- **RBAC**：Admin only
- **路徑參數**：`id`（userId）
- **Request Body**：無
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00001（使用者不存在）、UA00009（最後一個有效 Admin）、SB00002（停用自己）

---

### uaa-service (:8081)

#### GET /internal/users

- **說明**：查詢指定租戶的使用者清單（含站點綁定）
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<List<UaaUserRs>>`（欄位同 UserRs）

#### POST /internal/users

- **說明**：建立使用者帳號，同時寫入站點綁定
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：
  ```json
  {
    "account":     "jane@factory.com",
    "displayName": "Jane",
    "password":    "plain-text",
    "role":        "VIEWER",
    "stationIds":  ["S01"],
    "tenantId":    999
  }
  ```
- **Response**：`ApiResponse<UaaUserRs>`
- **錯誤情境**：UA00007

#### PUT /internal/users/{id}

- **說明**：更新使用者資訊與站點綁定
- **Request Header**：`X-Tenant-Id: {tenantId}`、`X-User-Id: {operatorId}`
- **Request Body**：
  ```json
  {
    "displayName": "Jane Wu",
    "role":        "MAINTAINER",
    "stationIds":  []
  }
  ```
- **Response**：`ApiResponse<UaaUserRs>`
- **錯誤情境**：UA00001

#### POST /internal/users/{id}/disable

- **說明**：停用帳號（純 DB 操作）
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00001、UA00009

---

## 3. 業務邏輯

### 3.1 建立使用者

```
POST /api/users (saas-bff)
  UseCase：
    ctx = UserContextHolder.get()
    role != ADMIN → throw SB00001

  呼叫 POST /internal/users (uaa)
    uaa @Transactional：
      a. 查 users WHERE account = ? → 存在 → UA00007
      b. BCrypt.hash(password)
      c. INSERT users（id = Snowflake, tenant_id, account, display_name, password_hash, role, status = ACTIVE）
      d. 若 stationIds 不為空：
           DELETE user_station_bindings WHERE user_id = ?（防重複）
           INSERT user_station_bindings (batch)
```

### 3.2 更新使用者

```
PUT /api/users/{id} (saas-bff)
  UseCase：
    ctx = UserContextHolder.get()
    role != ADMIN → throw SB00001
    ctx.userId == targetId && body.role != ctx.role → throw SB00003（不可修改自己的 role）

  呼叫 PUT /internal/users/{id} (uaa)
    uaa @Transactional：
      a. 查 users WHERE id = ? AND tenant_id = ? → 不存在 → UA00001
      b. UPDATE users SET display_name = ?, role = ?
      c. DELETE user_station_bindings WHERE user_id = ?
      d. 若 stationIds 不為空：INSERT user_station_bindings (batch)
      e. DEL user:stations:{id}  ← 讓 JwtAuthFilter 下次重新載入

  注意：role 改變後，對方現有 session 的 JWT 仍帶舊 role，需等 access token 過期（15 min）才生效。
        若需立即生效，可考慮在 access token blacklist 加入該用戶的 jti（TODO）。
```

### 3.3 停用帳號

```
POST /api/users/{id}/disable (saas-bff)
  UseCase：
    ctx = UserContextHolder.get()
    role != ADMIN             → throw SB00001
    ctx.userId == targetId    → throw SB00002（不可停用自己）

  呼叫 POST /internal/users/{id}/disable (uaa)
    uaa @Transactional：
      a. 查 users WHERE id = ? AND tenant_id = ? → 不存在 → UA00001
      b. 查 users WHERE tenant_id = ? AND status = 'ACTIVE' AND role = 'ADMIN' AND id != targetId
         → 結果為空（本人是最後一個 Active Admin） → UA00009
      c. UPDATE users SET status = 'DISABLED'

失效時序：
  - 現有 access token（最多 15 min）仍可使用
  - access token 到期後嘗試 refresh → uaa 查 users.status = DISABLED → UA00006
  - 用戶自然登出
```

### 3.4 站點綁定說明

- Admin：永遠不受站點限制，`user_station_bindings` 不存在其記錄
- Maintainer / Viewer 無綁定記錄（stationIds 空）= 可存取全部站點
- Maintainer / Viewer 有綁定記錄 = 只能看指定站點的資料
- saas-bff JwtAuthFilter 從 `user:stations:{userId}` 讀取（快取），填入 `UserContext.stationIds`
- `X-Station-Ids` header 由 saas-bff 帶入下游服務，格式為逗號分隔字串（例如 `"S01,S02"`）

---

## 4. 錯誤碼

> 本 spec 新增：UA00009、SB00001、SB00002、SB00003。

| 錯誤碼 | 說明 | HTTP | 服務 | 來源 |
|--------|------|------|------|------|
| `UA00001` | 使用者不存在 | 400 | uaa-service | 已存在 |
| `UA00006` | 帳號已停用（refresh 時觸發） | 400 | uaa-service | 已存在 |
| `UA00007` | 帳號（account）已存在 | 400 | uaa-service | 已存在 |
| `UA00009` | 至少需保留一個有效的 Admin 帳號，停用失敗 | 400 | uaa-service | **本 spec 新增** |
| `SB00001` | 無操作權限（非 Admin） | 400 | saas-bff | **本 spec 新增** |
| `SB00002` | 不可停用自己的帳號 | 400 | saas-bff | **本 spec 新增** |
| `SB00003` | 不可修改自己的角色 | 400 | saas-bff | **本 spec 新增** |

---

## 5. 待確認事項

- 密碼重設流程（PRD-005 標記為 TODO）：Admin 重設後直接設新密碼，還是發臨時密碼？
- 停用帳號後能否重新啟用？若需要，加一支 `POST /api/users/{id}/enable` API，純 DB 操作（UPDATE status = ACTIVE）即可。
