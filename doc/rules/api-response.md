# ApiResponse 統一回應封裝

`ApiResponse` 是所有服務 REST 端點的統一回應封裝（Unified Response Wrapper）。

**套件（Package）：** `com.willthx.common.model.rest`
**模組（Module）：** `common-model`

---

## 1. 結構

| 欄位 | 型別 | 說明 |
|---|---|---|
| `responseCode` | String | 成功時為 `SUCCESS_CODE`（`"00000"`）；失敗時為服務錯誤代碼（例如 `IC00001`） |
| `msg` | String | 人類可讀的描述訊息 |
| `data` | `T` / null | 回應資料；錯誤或無內容操作時為 `null` |

---

## 2. 類別定義

```java
public class ApiResponse<T> {

    public static final String SUCCESS_CODE = "00000";
    public static final String SUCCESS_MSG  = "ok";

    private String responseCode;
    private String msg;
    private T      data;

    public ApiResponse(String responseCode, String msg, T data) { ... }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    public static <T> ApiResponse<T> error(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(responseCode);
    }
}
```

所有對成功代碼的引用必須使用 `ApiResponse.SUCCESS_CODE`，字面值 `"00000"` 不得出現在此類別以外的地方。

---

## 3. 使用方式

### Controller — 帶資料的成功回應

```java
@PostMapping("/auth/login")
public ResponseEntity<ApiResponse<LoginRs>> login(@RequestBody @Valid LoginRq rq) {
    LoginRs rs = loginUseCase.login(rq);
    return ResponseEntity.ok(ApiResponse.success(rs));
}
```

### Controller — 不帶資料的成功回應（例如 ACK 告警、啟用設備）

```java
@PostMapping("/alerts/{id}/ack")
public ResponseEntity<ApiResponse<Void>> ack(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long id) {
    ackAlertUseCase.ack(userId, id);
    return ResponseEntity.ok(ApiResponse.success());
}
```

### 全域例外處理器 — 錯誤回應（由 GlobalExceptionHandler 自動處理）

```java
return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(
                error.getModelType().getCode() + error.getErrorCode(),
                error.getMessage()));
```

`GlobalExceptionHandler` 直接建構錯誤回應；Controller 本身不應建立錯誤回應。

---

## 4. WebSocket 回應格式

realtime-bff 的 WebSocket 推播訊息使用獨立格式（非 `ApiResponse`），
定義在 `com.willthx.realtime.model.WsMessage`：

```java
public class WsMessage<T> {
    private String type;    // "TELEMETRY" / "ALERT" / "HEARTBEAT"
    private T      payload;
    private long   ts;      // epoch millis
}
```

---

## 5. 規則

- 所有 REST Controller 方法必須回傳 `ResponseEntity<ApiResponse<T>>`
- 成功回應必須使用 `ApiResponse.success()` 或 `ApiResponse.success(data)`，不得手動建構
- 錯誤回應的 `data` 必須為 `null`，不得包含部分結果
- 不得為一次性需求在 `ApiResponse` 中增加欄位，應使用專用的 Rs DTO
- `SUCCESS_CODE` 是唯一的事實來源，其他地方不得使用魔術字串 `"00000"`
- Kafka / RabbitMQ 訊息不使用 `ApiResponse` 封裝，使用各自的 Event 格式
