# SPEC-3：功能管理與角色權限設定

> 對應 PRD：PRD-005
> 版本：v0.2
> 日期：2026-08-13
> 服務：uaa-service (:8081)、saas-bff (:8090)
> 狀態：草稿

---

## 1. 領域模型

### Entity

**features**（功能清單，支援父子兩層樹狀）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT NOT NULL | |
| `parent_id` | BIGINT | null = 父功能群組；非 null = 子功能（實際頁面） |
| `feature_code` | VARCHAR(100) NOT NULL | 功能識別碼，`UNIQUE(tenant_id, feature_code)` |
| `feature_name` | VARCHAR(100) NOT NULL | 顯示名稱 |
| `route` | VARCHAR(255) | 前端路由路徑；父功能為 null，子功能必填 |
| `sort_order` | INT NOT NULL | 排序（同層級內升冪） |
| `is_active` | BOOLEAN NOT NULL DEFAULT true | 軟停用 |
| `created_at` | TIMESTAMP NOT NULL | BaseTimeEntity |
| `updated_at` | TIMESTAMP NOT NULL | BaseTimeEntity |

> 只支援兩層：父功能（parent_id IS NULL）和子功能（parent_id IS NOT NULL）。不支援三層以上。

**role_feature_permissions**（角色 × 子功能授權矩陣）

| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | BIGINT (Snowflake) PK | |
| `tenant_id` | BIGINT NOT NULL | |
| `role` | VARCHAR(30) NOT NULL | 僅存 MAINTAINER / VIEWER |
| `feature_id` | BIGINT NOT NULL | 只對子功能（parent_id IS NOT NULL）設定 |
| — | UNIQUE(tenant_id, role, feature_id) | |

> ADMIN 自動擁有所有 is_active=true 的功能，程式碼層跳過此表查詢，DB 不存 ADMIN 記錄。
> 子功能刪除時，應用層負責同步刪除對應的 role_feature_permissions 記錄（不依賴 DB cascade）。

---

## 2. API 規格

> 所有端點回傳 `ResponseEntity<ApiResponse<T>>`
> 所有端點須通過 JwtAuthFilter；RBAC 在 saas-bff UseCase 層檢查（role != ADMIN → SB00001）

### saas-bff (:8090)

#### GET /api/features

- **說明**：查詢本租戶全部功能清單（含停用的），供管理頁面使用
- **RBAC**：Admin only
- **Response**：`ApiResponse<List<FeatureRs>>`，平坦陣列
  ```json
  [
    { "featureId": 1, "featureCode": "sys-settings",  "featureName": "系統設定",   "parentId": null, "route": null,    "sortOrder": 1, "isActive": true },
    { "featureId": 2, "featureCode": "user-mgmt",     "featureName": "使用者管理", "parentId": 1,    "route": "/users","sortOrder": 1, "isActive": true },
    { "featureId": 3, "featureCode": "feature-mgmt",  "featureName": "功能管理",   "parentId": 1,    "route": "/features","sortOrder": 2, "isActive": false }
  ]
  ```

#### POST /api/features

- **說明**：新增功能（父功能群組或子功能頁面）
- **RBAC**：Admin only
- **Request Body**：
  ```json
  {
    "parentId":    null,
    "featureCode": "alert-center",
    "featureName": "告警中心",
    "route":       null,
    "sortOrder":   3
  }
  ```
- **驗證規則**：
  - `parentId == null`（父功能）→ `route` 必須為 null
  - `parentId != null`（子功能）→ `route` 必填，且 parentId 必須指向存在的父功能
  - `featureCode` 在同租戶內唯一
- **Response**：`ApiResponse<FeatureRs>`
- **錯誤情境**：UA00010（feature_code 重複）、UA00011（parentId 指向不存在的功能）

#### PUT /api/features/{id}

- **說明**：更新功能顯示資訊（`parentId`、`featureCode` 建立後不可修改）
- **RBAC**：Admin only
- **Request Body**：
  ```json
  {
    "featureName": "告警管理中心",
    "route":       "/alerts",
    "sortOrder":   3
  }
  ```
- **Response**：`ApiResponse<FeatureRs>`
- **錯誤情境**：UA00011（功能不存在）

#### PUT /api/features/{id}/active

- **說明**：切換功能啟用 / 停用狀態（軟停用）
- **RBAC**：Admin only
- **Request Body**：`{ "active": false }`
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00011

#### DELETE /api/features/{id}

- **說明**：永久刪除功能（硬刪除，同步清除 role_feature_permissions）
- **RBAC**：Admin only
- **路徑參數**：`id`（featureId）
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00011（功能不存在）、UA00012（父功能下仍有子功能）

#### GET /api/role-permissions

- **說明**：查詢 MAINTAINER 與 VIEWER 的功能權限矩陣（供角色權限設定頁使用）
- **RBAC**：Admin only
- **Response**：`ApiResponse<RolePermissionsRs>`
  ```json
  {
    "features": [
      { "featureId": 2, "featureCode": "user-mgmt",    "featureName": "使用者管理", "parentName": "系統設定" },
      { "featureId": 3, "featureCode": "feature-mgmt", "featureName": "功能管理",   "parentName": "系統設定" }
    ],
    "permissions": {
      "MAINTAINER": [2],
      "VIEWER":     []
    }
  }
  ```
  > `features` 只列子功能（parent_id IS NOT NULL）；ADMIN 欄位前端自行補全，不需後端回傳。

#### PUT /api/role-permissions/{role}

- **說明**：全量更新指定角色的子功能權限（刪舊全部 + 批次寫新，單一 transaction）
- **RBAC**：Admin only
- **路徑參數**：`role`（`MAINTAINER` 或 `VIEWER`）
- **Request Body**：
  ```json
  { "featureIds": [2, 5, 7] }
  ```
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00013（featureId 不存在或不屬於此租戶）、UA00014（包含父功能 ID）

---

### uaa-service (:8081)

#### GET /internal/features

- **說明**：查詢租戶全部功能（含停用）
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<List<UaaFeatureRs>>`（欄位同 FeatureRs）

#### POST /internal/features

- **說明**：建立功能
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：同 saas-bff POST /api/features Request Body
- **Response**：`ApiResponse<UaaFeatureRs>`
- **錯誤情境**：UA00010、UA00011

#### PUT /internal/features/{id}

- **說明**：更新功能資訊
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：featureName、route、sortOrder
- **Response**：`ApiResponse<UaaFeatureRs>`
- **錯誤情境**：UA00011

#### PUT /internal/features/{id}/active

- **說明**：切換 is_active
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：`{ "active": false }`
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00011

#### DELETE /internal/features/{id}

- **說明**：刪除功能 + 同步清除 role_feature_permissions（應用層 cascade，一個 @Transactional）
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00011、UA00012

#### GET /internal/role-permissions

- **說明**：查詢角色權限矩陣
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Response**：`ApiResponse<UaaRolePermissionsRs>`

#### PUT /internal/role-permissions/{role}

- **說明**：全量更新角色權限
- **Request Header**：`X-Tenant-Id: {tenantId}`
- **Request Body**：`{ "featureIds": [2, 5, 7] }`
- **Response**：`ApiResponse<Void>`
- **錯誤情境**：UA00013、UA00014

#### GET /internal/features/tree

- **說明**：依 role 查詢已過濾的功能平坦清單（登入時由 saas-bff 呼叫，亦可用於 session 刷新）
- **Request Header**：`X-Tenant-Id: {tenantId}`、`X-User-Role: {role}`
- **Response**：`ApiResponse<List<UaaFeatureRs>>`（平坦陣列，已依 role 過濾與排序）

---

## 3. 業務邏輯

### 3.1 新增功能

```
POST /api/features (saas-bff)
  UseCase：
    ctx.role != ADMIN → SB00001

  呼叫 POST /internal/features (uaa)
    uaa @Transactional：
      a. 驗證 featureCode 在 tenant 內唯一 → UA00010
      b. 若 parentId != null：查 features WHERE id = parentId AND tenant_id = ? → 不存在 → UA00011
      c. 若 parentId != null 且 route 為空 → 應用層驗證錯誤（400 Bad Request）
      d. 若 parentId == null 且 route 非空 → 應用層驗證錯誤
      e. INSERT features
```

### 3.2 刪除功能

```
DELETE /api/features/{id} (saas-bff)
  UseCase：
    ctx.role != ADMIN → SB00001

  呼叫 DELETE /internal/features/{id} (uaa)
    uaa @Transactional：
      a. 查 features WHERE id = ? AND tenant_id = ? → 不存在 → UA00011
      b. 若 parent_id IS NULL（父功能）：
           查 features WHERE parent_id = ? AND tenant_id = ? → 有子功能 → UA00012
      c. 若 parent_id IS NOT NULL（子功能）：
           DELETE role_feature_permissions WHERE feature_id = ? AND tenant_id = ?
      d. DELETE features WHERE id = ? AND tenant_id = ?
```

### 3.3 全量更新角色權限

```
PUT /api/role-permissions/{role} (saas-bff)
  UseCase：
    ctx.role != ADMIN → SB00001
    role path param 必須為 MAINTAINER 或 VIEWER（ADMIN 禁止）→ SB00001

  呼叫 PUT /internal/role-permissions/{role} (uaa)
    uaa @Transactional：
      a. 驗證所有 featureIds 存在且屬於 tenant → UA00013
      b. 驗證所有 featureIds 對應的 features.parent_id IS NOT NULL → UA00014
      c. DELETE role_feature_permissions WHERE tenant_id = ? AND role = ?
      d. INSERT role_feature_permissions (batch，id 各自 Snowflake)
```

### 3.4 停用功能 vs 刪除功能

| 操作 | 效果 | 可復原 |
|------|------|--------|
| `PUT /active { "active": false }` | 所有角色的功能清單不再顯示，role_feature_permissions 保留 | 是，重新 active=true 即恢復 |
| `DELETE` | 功能記錄永久移除，role_feature_permissions 同步清除 | 否 |

---

## 4. 錯誤碼

> 本 spec 新增：UA00010、UA00011、UA00012、UA00013、UA00014。

| 錯誤碼 | UaaErrorType | 說明 | HTTP | 來源 |
|--------|-------------|------|------|------|
| `UA00010` | `FEATURE_CODE_DUPLICATE` | feature_code 在此租戶已存在 | 400 | **本 spec 新增** |
| `UA00011` | `FEATURE_NOT_FOUND` | 功能不存在或不屬於此租戶 | 400 | **本 spec 新增** |
| `UA00012` | `PARENT_HAS_CHILDREN` | 父功能下仍有子功能，請先刪除子功能 | 400 | **本 spec 新增** |
| `UA00013` | `FEATURE_ID_INVALID` | featureId 不存在或不屬於此租戶 | 400 | **本 spec 新增** |
| `UA00014` | `PERMISSION_LEAF_ONLY` | 只能對子功能（非父功能）設定角色權限 | 400 | **本 spec 新增** |
| `SB00001` | `FORBIDDEN` | 無操作權限（非 Admin） | 400 | 沿用 SPEC-2 |

---

## 5. 待確認事項

- 停用某功能（is_active=false）後，已取得此功能的 session 是否立即失效？目前設計：下次登入才生效（不影響現有 JWT）。
- 功能樹是否需要超過兩層（目前限制父子兩層）？若需三層以上，parent 的 parent 需額外設計。
- `PUT /api/role-permissions/{role}` 更新後，已登入用戶的功能清單何時更新？目前設計：下次登入才刷新。是否需要 push 通知前端強制重新整理？
- SB00001 定義於 SPEC-2，本 spec 沿用，實作時確認 `SaasBffErrorType` 已包含此碼。
