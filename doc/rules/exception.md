# 例外處理（Exception Handling）

系統採用分層例外處理架構，每個服務定義自己的錯誤類型，透過統一的全域處理器（GlobalExceptionHandler）轉換為標準化回應。

核心元件：

- `AbstractServiceException`：基底例外類別，定義在 `common-model`
- `BaseErrorType`：錯誤類型介面，定義在 `common-model`
- `IErrorLevel`：錯誤等級列舉，定義在 `common-model`
- `ModelType`：模組類型列舉，定義在 `common-model`
- 服務特定例外（例如 `IotCoreException`）：定義在各 `*-service` 內
- `GlobalExceptionHandler`：定義在 `common-web`

---

## 1. AbstractServiceException

```java
public class AbstractServiceException extends RuntimeException {
    private final String     errorCode;
    private final IErrorLevel errorLevel;
    private final ModelType  modelType;
    private final String     memo;

    public AbstractServiceException(BaseErrorType errorType, String exceptionMsg) {
        super(exceptionMsg);
        this.errorCode  = errorType.getCustomErrorCode();
        this.errorLevel = errorType.getIErrorLevel();
        this.modelType  = errorType.getModelType();
        this.memo       = errorType.getMemo();
    }
    // Getters...
}
```

---

## 2. ModelType 列舉

回傳給客戶端的 `responseCode` 由 `modelType.getCode() + errorCode` 組成，例如 `IC00001`。

```java
public enum ModelType {
    UA("UA"),   // uaa-service
    IC("IC"),   // iotcore-service
    TE("TE"),   // telemetry-service
    IA("IA"),   // iot-adapter
    SB("SB"),   // saas-bff
    RB("RB");   // realtime-bff

    private final String code;
    ModelType(String code) { this.code = code; }
    public String getCode() { return code; }
}
```

所有對模組前綴的引用必須使用 `ModelType` 列舉值，不得硬編碼字串 `"IC"` 等。

---

## 3. IErrorLevel 列舉

```java
public enum IErrorLevel {
    LOW,   // 業務邏輯錯誤（例如資源不存在、規則衝突）
    HIGH   // 系統錯誤（例如資料庫連線失敗、訊息發布失敗）
}
```

HIGH 等級錯誤在 `GlobalExceptionHandler` 中以 `log.error` 記錄；LOW 以 `log.warn` 記錄。

---

## 4. 服務特定例外（各服務定義）

以 `iotcore-service` 為例：

```java
public class IotCoreException extends AbstractServiceException {

    public enum IotCoreErrorType implements BaseErrorType {
        STATION_NOT_FOUND    ("00001", IErrorLevel.LOW,  ModelType.IC, "Station not found"),
        MACHINE_NOT_FOUND    ("00002", IErrorLevel.LOW,  ModelType.IC, "Machine not found"),
        ALERT_RULE_CONFLICT  ("00003", IErrorLevel.LOW,  ModelType.IC, "Alert rule already exists"),
        LOT_NOT_FOUND        ("00004", IErrorLevel.LOW,  ModelType.IC, "Lot not found"),
        UNIT_STATE_INVALID   ("00005", IErrorLevel.LOW,  ModelType.IC, "Invalid unit state transition"),
        OUTBOX_PUBLISH_FAILED("00006", IErrorLevel.HIGH, ModelType.IC, "Outbox publish failed");

        private final String      code;
        private final IErrorLevel level;
        private final ModelType   modelType;
        private final String      memo;

        IotCoreErrorType(String code, IErrorLevel level, ModelType modelType, String memo) {
            this.code      = code;
            this.level     = level;
            this.modelType = modelType;
            this.memo      = memo;
        }

        @Override public String      getCustomErrorCode() { return code; }
        @Override public String      getMemo()             { return memo; }
        @Override public IErrorLevel getIErrorLevel()     { return level; }
        @Override public ModelType   getModelType()       { return modelType; }
    }

    public IotCoreException(BaseErrorType errorType) {
        super(errorType, errorType.getMemo());
    }
}
```

### 各服務例外類別與 ModelType 對應

| 服務 | 例外類別 | ModelType |
|---|---|---|
| uaa-service | `UaaException` | `UA` |
| iotcore-service | `IotCoreException` | `IC` |
| telemetry-service | `TelemetryException` | `TE` |
| iot-adapter | `IotAdapterException` | `IA` |
| saas-bff | `SaasBffException` | `SB` |
| realtime-bff | `RealtimeBffException` | `RB` |

---

## 5. 使用範例

```java
@Override
@Transactional
public LotModel getLot(Long tenantId, Long lotId) {
    LotEntity lot = lotRepository.findByIdAndTenantId(lotId, tenantId)
            .orElseThrow(() -> new IotCoreException(IotCoreErrorType.LOT_NOT_FOUND));

    // 多租戶隔離已由 findByIdAndTenantId 確保，不再額外判斷
    return toModel(lot);
}

@Override
@Transactional
public void transitionUnitState(Long unitId, UnitState targetState) {
    UnitEntity unit = unitRepository.findById(unitId)
            .orElseThrow(() -> new IotCoreException(IotCoreErrorType.UNIT_STATE_INVALID));

    if (!unit.getState().canTransitionTo(targetState)) {
        throw new IotCoreException(IotCoreErrorType.UNIT_STATE_INVALID);
    }
    // 繼續處理...
}
```

---

## 6. GlobalExceptionHandler（common-web）

```java
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException error) {
        String msg = error.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("SB00000", msg));
    }

    @ExceptionHandler(AbstractServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(
            AbstractServiceException error) {
        if (error.getErrorLevel() == IErrorLevel.HIGH) {
            log.error("[{}{}] {}", error.getModelType().getCode(),
                    error.getErrorCode(), error.getMessage());
        } else {
            log.warn("[{}{}] {}", error.getModelType().getCode(),
                    error.getErrorCode(), error.getMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(
                        error.getModelType().getCode() + error.getErrorCode(),
                        error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception error) {
        log.error("Unexpected error", error);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("SB99999", "Internal server error"));
    }
}
```

---

## 7. Feign 例外處理（saas-bff）

服務間通訊使用 **HTTP 200 涵蓋成功與領域錯誤**，`ApiResponse.responseCode` 承載結果。
`FeignException` 僅在真正的基礎設施故障（5xx、逾時）時被拋出。

saas-bff 的 Feign Adapter 必須：

1. 呼叫 Feign API 後檢查 `response.isSuccess()`
2. 將已知錯誤代碼映射到 BFF 例外類型，不得硬編碼字串
3. 先捕獲 BFF 特定例外再拋出，最後捕獲 `FeignException`

```java
@Override
public LotDetailDto getLotDetail(UserContext ctx, Long lotId) {
    try {
        ApiResponse<LotDetailRs> response = iotcoreFeignApi.getLotDetail(
                ctx.getUserId(), ctx.getTenantId(), lotId);

        if (!response.isSuccess()) {
            // 依 responseCode 映射到具體錯誤
            if ((ModelType.IC.getCode() + "00004").equals(response.getResponseCode())) {
                throw new SaasBffException(SaasBffErrorType.LOT_NOT_FOUND);
            }
            throw new SaasBffException(SaasBffErrorType.DOWNSTREAM_ERROR);
        }

        return toDto(response.getData());
    } catch (SaasBffException e) {
        throw e;
    } catch (FeignException e) {
        log.error("iotcore-service unavailable: status={}", e.status());
        throw new SaasBffException(SaasBffErrorType.SERVICE_UNAVAILABLE);
    }
}
```

---

## 8. IoT 特有：Kafka Consumer 例外處理

Kafka Consumer 不可讓例外外溢導致 Offset 提交失敗（避免無限重試）：

```java
@KafkaListener(topics = "telemetry.#", groupId = "telemetry-writer")
public void onTelemetry(ConsumerRecord<String, String> record) {
    try {
        // 處理邏輯
    } catch (IotCoreException e) {
        log.warn("Business error processing telemetry: {}", e.getMessage());
        // 業務錯誤：記錄並跳過（不重試）
    } catch (Exception e) {
        log.error("Unexpected error processing telemetry key={}", record.key(), e);
        // 基礎設施錯誤：可考慮發送到 Dead Letter Topic
    }
}
```

---

## 9. 模組位置摘要

| 類別 | 位置 |
|---|---|
| `AbstractServiceException` | `common-model` |
| `BaseErrorType` | `common-model` |
| `IErrorLevel` | `common-model` |
| `ModelType` | `common-model` |
| `GlobalExceptionHandler` | `common-web` |
| `IotCoreException` 等 | 各 `*-service` 內部 |
| `SaasBffException` | `saas-bff` |
| `RealtimeBffException` | `realtime-bff` |
