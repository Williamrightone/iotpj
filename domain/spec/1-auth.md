# SPEC-1：認證（登入 / 登出 / Token 刷新）

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
```

### Entity（僅列本 spec 相關欄位）

**users**

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT | 所屬租戶；null = 平台層超管 |
| `account` | VARCHAR(100) NOT NULL | Email 格式，全系統唯一 |
| `display_name` | VARCHAR(100) NOT NULL | 顯示姓名 |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt |
| `role` | VARCHAR(30) NOT NULL | ADMIN / MAINTAINER / VIEWER |
| `status` | VARCHAR(20) NOT NULL | ACTIVE / DISABLED |
| `last_login_at` | TIMESTAMP | 最後登入時間 |

> **注意**：V1.0.0 已建立 users 表，欄位名稱與上述不同（username/name/active）。V1.0.1 Migration 執行 ALTER 對齊。

### Redis Key

| Key | TTL | Value | 說明 |
|-----|-----|-------|------|
| `auth:refresh:{jti}` | 7 天（604800s） | `{"userId":..,"tenantId":..}` JSON | refresh token 存活標記 |
| `auth:blacklist:{jti}` | access token 剩餘秒數 | `"1"` | access token 強制失效（登出用） |
| `user:stations:{userId}` | 1 小時（3600s） | `["S01","S02"]` JSON | 站點綁定快取，空陣列表示可存取全部站點 |

---

## 2. API 規格

> 所有端點回傳 `ResponseEntity<ApiResponse<T>>`，遵循 doc/rules/api-response.md

### saas-bff (:8090)

#### POST /api/auth/login

- **說明**：帳密登入，回傳 JWT 與可存取功能清單
- **JwtAuthFilter**：跳過（公開端點）
- **Request Body**：
  ```json
  {
    "account":  "john@factory.com",
    "password": "plain-text"
  }
  ```
- **Response**：`ApiResponse<LoginRs>`
  ```json
  {
    "responseCode": "00000",
    "msg": "ok",
    "data": {
      "accessToken":  "eyJ...",
      "refreshToken": "eyJ...",
      "user": {
        "userId":      1234567890,
        "account":     "john@factory.com",
        "displayName": "John",
        "role":        "ADMIN",
        "tenantId":    999
      },
      "features": [
        { "featureId": 1, "featureCode": "sys-settings", "featureName": "系統設定",   "parentId": null, "route": null,              "sortOrder": 1 },
        { "featureId": 2, "featureCode": "user-mgmt",    "featureName": "使用者管理", "parentId": 1,    "route": "/users",           "sortOrder": 1 },
        { "featureId": 3, "featureCode": "feature-mgmt", "featureName": "功能管理",   "parentId": 1,    "route": "/features",        "sortOrder": 2 }
      ]
    }
  }
  ```
  > `features` 為平坦陣列（非巢狀），前端依 `parentId` 自行組裝側邊欄樹狀結構。父功能 `route` 為 null，子功能 `route` 為前端路由路徑。
- **錯誤情境**：UA00002（帳號或密碼錯誤）、UA00006（帳號已停用）

#### POST /api/auth/logout

- **說明**：登出，撤銷 refresh token 並將 access token 加入黑名單
- **JwtAuthFilter**：需通過
- **Request Header**：`Authorization: Bearer {accessToken}`
- **Request Body**：
  ```json
  { "refreshToken": "eyJ..." }
  ```
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：無（refresh token 不存在時靜默忽略）

#### POST /api/auth/refresh

- **說明**：以 refresh token 換取新的 access token（refresh token 本身不更換）
- **JwtAuthFilter**：跳過（公開端點）
- **Request Body**：
  ```json
  { "refreshToken": "eyJ..." }
  ```
- **Response**：`ApiResponse<RefreshRs>`
  ```json
  { "accessToken": "eyJ..." }
  ```
- **錯誤情境**：UA00005（refresh token 無效或已過期）

---

### uaa-service (:8081)

#### POST /internal/auth/login

- **說明**：驗證帳密 → 核發 JWT → 寫 Redis → 回傳使用者資訊與功能清單（一次完成）
- **Request Header**：無（internal call，不帶 JWT）
- **Request Body**：
  ```json
  { "account": "john@factory.com", "password": "plain-text" }
  ```
- **Response**：`ApiResponse<UaaLoginRs>`
  ```json
  {
    "accessToken":  "eyJ...",
    "refreshToken": "eyJ...",
    "userId":       1234567890,
    "account":      "john@factory.com",
    "displayName":  "John",
    "role":         "ADMIN",
    "tenantId":     999,
    "features":     [ ... ]
  }
  ```
- **錯誤情境**：UA00002、UA00006

#### POST /internal/auth/logout

- **說明**：撤銷 refresh token + 將 access token jti 加入黑名單
- **Request Body**：
  ```json
  {
    "accessJti":       "uuid-of-access-token",
    "accessRemainingSeconds": 843,
    "refreshJti":      "uuid-of-refresh-token"
  }
  ```
- **Response**：`ApiResponse<Void>`

#### POST /internal/auth/refresh

- **說明**：驗 refresh token，核發新 access token
- **Request Body**：`{ "refreshToken": "eyJ..." }`
- **Response**：`ApiResponse<UaaRefreshRs>`
  ```json
  { "accessToken": "eyJ..." }
  ```
- **錯誤情境**：UA00005

#### GET /internal/auth/jwks

- **說明**：回傳 RS256 公鑰（JWK Set 格式），供 saas-bff 啟動時快取
- **Response**：JWK Set JSON（標準格式）

---

## 3. 業務邏輯

### 3.1 登入流程

```
POST /api/auth/login (saas-bff)
  │
  └─ 呼叫 POST /internal/auth/login (uaa)
       uaa Domain Service 執行：
         a. SELECT users WHERE account = ?
         b. 查無此帳號 or BCrypt 比對失敗 → UA00002（統一錯誤，不分帳號/密碼）
         c. status = DISABLED → UA00006
         d. 產生 accessToken（RS256，15 min）
              payload: { userId, account, role, tenantId, jti, exp }
         e. 產生 refreshToken（RS256，7 天）
              payload: { userId, tenantId, jti }
         f. SET auth:refresh:{refreshJti} EX 604800
              value = {"userId": .., "tenantId": ..}
         g. UPDATE users SET last_login_at = NOW()
         h. SELECT user_station_bindings WHERE user_id = ?
         i. SET user:stations:{userId} EX 3600
              value = ["S01","S02"]  // 空陣列表示全站點
         j. 依 role 查詢功能清單（見 3.2）
         k. 回傳 UaaLoginRs
  │
  saas-bff 組裝 LoginRs 回傳前端
```

### 3.2 Feature Tree 過濾規則

```
ADMIN：
  SELECT * FROM features
  WHERE tenant_id = ? AND is_active = true
  ORDER BY sort_order ASC

其他角色（MAINTAINER / VIEWER）：
  1. SELECT feature_id FROM role_feature_permissions
     WHERE tenant_id = ? AND role = ?
  2. SELECT * FROM features
     WHERE id IN (...) AND is_active = true          -- 子功能
  3. 補上步驟 2 中各子功能的 parent 節點
     WHERE id IN (子功能的 parent_id set) AND is_active = true
  4. 合併後依 sort_order ASC 回傳平坦陣列

規則：父功能下若無任何可見子功能，父功能節點不回傳。
```

### 3.3 登出流程

```
POST /api/auth/logout (saas-bff)
  1. JwtAuthFilter 已解析 accessToken，ctx.jti 與 ctx.exp 已取得
  2. 解析 request body 取得 refreshToken → 解碼得 refreshJti
  3. 呼叫 POST /internal/auth/logout (uaa)
       uaa 執行：
         a. DEL  auth:refresh:{refreshJti}
         b. SET  auth:blacklist:{accessJti}  EX {accessRemainingSeconds}
```

### 3.4 Token 刷新流程

```
POST /api/auth/refresh (saas-bff)
  1. 呼叫 POST /internal/auth/refresh (uaa)
       uaa 執行：
         a. 解析 refreshToken JWT，取得 jti
         b. GET auth:refresh:{jti}  → key 不存在 → UA00005
         c. 核發新 accessToken（refreshToken 本身不換）
         d. 回傳新 accessToken
```

### 3.5 JwtAuthFilter 驗證步驟（saas-bff）

| 步驟 | 動作 | 失敗時 |
|------|------|--------|
| 1 | 從 `Authorization` 提取 Bearer token；缺少或格式錯誤 | 401 |
| 2 | 使用 uaa RS256 公鑰驗簽 | 401 |
| 3 | 檢查 `exp` | 401 |
| 4 | GET `auth:blacklist:{jti}` 存在 → token 已登出 | 401 |
| 5 | 解析 claims → UserContext（userId、role、tenantId、jti、exp） | 401 |
| 6 | role = MAINTAINER 或 VIEWER：GET `user:stations:{userId}`，填入 stationIds | — |
| 7 | UserContextHolder.set(ctx) | — |
| 8 | chain.doFilter() | — |
| 9（finally） | UserContextHolder.clear() | — |

---

## 4. 錯誤碼

> 本 spec 未新增錯誤碼，全部沿用 V1.0.0 既有定義。

| 錯誤碼 | UaaErrorType | 說明 | HTTP | 來源 |
|--------|-------------|------|------|------|
| `UA00002` | `INVALID_CREDENTIALS` | 帳號或密碼錯誤（統一訊息，不區分） | 400 | 已存在 |
| `UA00005` | `REFRESH_TOKEN_NOT_FOUND` | refresh token 無效或已過期 | 400 | 已存在 |
| `UA00006` | `ACCOUNT_DISABLED` | 帳號已停用，請聯繫管理員 | 400 | 已存在 |

---

## 5. 待確認事項

- 前端是否實作 silent refresh（access token 快到期時自動換新）？若是，需約定攔截器的重試機制。
- `tenant_id` 在 users 中目前允許 null（平台超管）；業務使用者的 tenant_id 是否應強制 NOT NULL？
