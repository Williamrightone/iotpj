# 身份驗證上下文（Auth Context）

本文件定義 willThx 後端的身份驗證上下文傳播策略：從 saas-bff 的 JWT 驗證，經由內部標頭傳播，到下游業務服務（iotcore-service、telemetry-service、uaa-service）。

## 設計原則

- **ThreadLocal 身份僅存在於 saas-bff。** saas-bff 驗證 JWT、填入 `UserContextHolder`，並在每個 UseCase 進入點取出 `UserContext`。
- **跨網路傳輸時，身份是最小化、明確的 HTTP 標頭契約**（`X-User-Id`、`X-User-Role`、`X-Tenant-Id`）。
- **業務服務內部，身份是明確的方法參數**，沿著 Controller → UseCase / DomainService 傳遞。業務服務沒有 `UserContextHolder`，不讀取 ThreadLocal。

---

## 1. Role 列舉

```java
public enum Role {
    ADMIN,
    OPERATOR,
    VIEWER
}
```

**位置：** `common-model`。所有模組匯入。使用列舉同一性（`==`）比較，不得與字串字面值比較。

---

## 2. UserContext

`UserContext` 僅存在於 saas-bff，儲存在 ThreadLocal 中。

```java
public class UserContext {
    private Long   userId;      // 使用者 ID（Snowflake）
    private String username;    // 登入帳號
    private String name;        // 顯示姓名
    private Role   role;        // ADMIN / OPERATOR / VIEWER
    private Long   tenantId;    // 租戶 ID（工廠）
    private String jti;         // JWT ID，用於定向登出
    private long   exp;         // 過期時間（epoch 秒）
}
```

```java
public class UserContextHolder {
    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    public static void set(UserContext ctx) { HOLDER.set(ctx); }
    public static UserContext get()         { return HOLDER.get(); }
    public static void clear()              { HOLDER.remove(); }
}
```

**位置：** `common-model`。生產程式碼中僅由 saas-bff 模組引用；業務服務不得匯入 `UserContextHolder`。

### 規則

- 只有 `JwtAuthFilter` 呼叫 `UserContextHolder.set(...)`
- 只有 UseCase 實作的 `execute(...)` 頂部呼叫 `UserContextHolder.get()`，取一次後以參數傳遞
- Port 介面及其 Adapter 將 `UserContext` 作為明確的第一個參數
- 業務服務不得讀取 `UserContextHolder`

---

## 3. saas-bff — JwtAuthFilter

繼承 `OncePerRequestFilter`，在每個受保護請求前執行。

**套件：** `bootstrap.filter`，位於 saas-bff

| 步驟 | 動作 |
|---|---|
| 1 | 從 `Authorization` 標頭提取 `Bearer` Token；缺少或格式錯誤則拒絕 |
| 2 | 使用 uaa-service 公鑰驗證 RS256 簽章；簽章錯誤則拒絕 |
| 3 | 檢查 Token 過期（`exp` Claim）；已過期則拒絕 |
| 4 | 查詢 Redis `jwt:blacklist:{jti}`；存在則拒絕（已登出） |
| 5 | 解析 Claims 為 `UserContext`（含 `Role.valueOf(claim.role)`、`tenantId`） |
| 6 | 呼叫 `UserContextHolder.set(ctx)`，設定 `SecurityContextHolder` |
| 7 | 呼叫 `FilterChain.doFilter()` |
| 8 | `finally`：`UserContextHolder.clear()` + `SecurityContextHolder.clearContext()` |

### 跳過路徑（公開端點）

```
POST /api/auth/login
POST /api/auth/refresh
GET  /actuator/**
```

### 公鑰取得

saas-bff 在啟動時呼叫 `GET /internal/auth/jwks`（uaa-service）取得 RS256 公鑰，
快取於 Spring Bean（或 Redis），每 24 小時更新一次。

---

## 4. RBAC 閘控（saas-bff）

RBAC 在 UseCase 層實施，不在 Controller：

```java
@Override
public void execute(CreateAlertRuleRq rq) {
    UserContext ctx = UserContextHolder.get();
    if (ctx.getRole() != Role.ADMIN) {
        throw new SaasBffException(SaasBffErrorType.FORBIDDEN);
    }
    // ...
}
```

| 功能 | ADMIN | OPERATOR | VIEWER |
|---|---|---|---|
| 站點/機台/IoT 元件設定（寫） | ✅ | ✗ | ✗ |
| 告警規則設定 | ✅ | ✗ | ✗ |
| 告警 Acknowledge | ✅ | ✅ | ✗ |
| 資料查詢 / 追溯 | ✅ | ✅ | ✅ |
| 使用者管理 | ✅ | ✗ | ✗ |
| 通知設定 | ✅ | ✗ | ✗ |

---

## 5. saas-bff → 業務服務：內部請求標頭

saas-bff 透過 Feign Client 的明確 `@RequestHeader` 參數轉發身份，**不使用** Feign RequestInterceptor 從 ThreadLocal 讀取。

| 標頭 | 型別 | 用途 |
|---|---|---|
| `X-User-Id` | `Long` | 所有已認證端點 |
| `X-User-Role` | `String` | 需要角色判斷的端點 |
| `X-Tenant-Id` | `Long` | 多租戶隔離（每個請求都帶） |

```java
@FeignClient(name = "iotcore-service", url = "${feign.iotcore.url}")
public interface IotcoreFeignApi {

    @GetMapping("/internal/lots/{lotId}")
    ApiResponse<LotDetailRs> getLotDetail(
            @RequestHeader("X-User-Id")   Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable("lotId")        Long lotId);
}
```

Adapter 從 `UserContext` 提取標頭：

```java
@Override
public LotDetailDto getLotDetail(UserContext ctx, Long lotId) {
    return iotcoreFeignApi.getLotDetail(
            ctx.getUserId(),
            ctx.getTenantId(),
            lotId
    ).getData();
}
```

---

## 6. 業務服務 — 接收身份

業務服務沒有 JwtAuthFilter，也不填入 UserContextHolder。
Controller 直接以 `@RequestHeader` 讀取身份。

```java
@RestController
@RequestMapping("/internal/lots")
@RequiredArgsConstructor
public class LotController {

    private final GetLotDetailUseCase getLotDetailUseCase;

    @GetMapping("/{lotId}")
    public ResponseEntity<ApiResponse<LotDetailRs>> getLotDetail(
            @RequestHeader("X-User-Id")   Long userId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable                 Long lotId) {
        return ResponseEntity.ok(ApiResponse.success(
                getLotDetailUseCase.execute(userId, tenantId, lotId)));
    }
}
```

### 業務服務身份規則

- 每個需要身份的 UseCase `execute(...)` 以 `Long userId`、`Long tenantId` 作為前兩個參數
- Domain Service 同樣以明確參數接收，不讀取 ThreadLocal
- 多租戶資料隔離：所有查詢都必須帶入 `tenantId` 條件

---

## 7. iot-adapter 與 realtime-bff 認證

- **iot-adapter**：無需 JWT，EMQX 連線使用 MQTT 帳密或 TLS 客戶端憑證驗證
- **realtime-bff WebSocket**：在 WebSocket 握手（HTTP Upgrade）時驗證 JWT（同 JwtAuthFilter 邏輯），握手成功後 Session 儲存於 Redis

---

## 8. 請求生命週期摘要

```text
Browser 請求
    │
JwtAuthFilter（saas-bff）
    │── 驗證 JWT RS256（UAA 公鑰）
    │── 解析 Role Enum、tenantId
    │── UserContextHolder.set(ctx)   ← ThreadLocal 僅在 saas-bff 程序內
    │
Controller → UseCase.execute()
    │── ctx = UserContextHolder.get()  ← 在 UseCase 進入點取一次
    │── rbac check（ctx.getRole()）
    │── port.method(ctx, ...)           ← 明確參數傳遞
    │
Feign Adapter
    │── 讀取 ctx.getUserId() / getTenantId() / getRole().name()
    │── 宣告為 @RequestHeader 參數
    │
業務服務 Controller
    │── @RequestHeader("X-User-Id") Long userId
    │── @RequestHeader("X-Tenant-Id") Long tenantId
    │── useCase.execute(userId, tenantId, ...)
    │
業務服務 Domain Service
    │── userId, tenantId 是一路向下的普通方法參數
    │
saas-bff finally: UserContextHolder.clear()
```

---

## 9. 模組位置摘要

| 類別 | 模組 | 說明 |
|---|---|---|
| `Role` | `common-model` | 共用列舉，所有模組匯入 |
| `UserContext` | `common-model` | 生產程式碼僅由 saas-bff 使用 |
| `UserContextHolder` | `common-model` | ThreadLocal 封裝，僅由 saas-bff 使用 |
| `JwtAuthFilter` | `saas-bff` | JWT 驗證，僅在 saas-bff |
| `*FeignApi` | `saas-bff` | 每個方法宣告明確 `@RequestHeader` |
| `*FeignClient`（Port Adapter） | `saas-bff` | 從 `UserContext` 提取標頭 |
| 下游 Controller | `*-service` | 讀取 `@RequestHeader("X-User-Id")` 等 |
