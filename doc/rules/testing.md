# 單元測試規範（Unit Testing）

## 1. 指導理念

willThx 採用清潔架構（Clean Architecture）。不強制 TDD，但 JaCoCo 確保所有業務關鍵層在合併前都經過驗證。

覆蓋率追蹤範圍限於業務邏輯所在的層：Domain Service、UseCase 實作、Adapter。基礎設施膠合程式碼（Controller、DTO、Bootstrap Configuration）明確排除在指標外，但在適當時仍需測試。

---

## 2. 覆蓋範圍

| 層別 | 套件 | 追蹤覆蓋率 | 說明 |
|---|---|---|---|
| Domain Service | `domain/service/**` | **是** | 必須涵蓋所有規格場景 |
| UseCase 實作 | `application/usecase/**` | **是** | 僅 BFF |
| Adapter — 持久化 | `adapter/persistence/repository/**` | **是** | |
| Adapter — Feign Client 實作 | `adapter/client/*FeignClient` | **是** | Mock 底層 FeignApi，驗證錯誤映射 |
| Adapter — Feign API（宣告式） | `adapter/client/*FeignApi` | 否 | 純 @FeignClient 介面，無邏輯，JaCoCo 排除 |
| Adapter — Kafka / RabbitMQ | `adapter/messaging/**` | **是** | 見 Docker 跳過規則 |
| Adapter — MQTT（iot-adapter） | `adapter/mqtt/**` | **是** | Testcontainers 或 MockMqttClient |
| Controller | `application/api/controller/**` | 否 | 有測試但排除在指標外 |
| DTO / Contract | `application/api/dto/**` | 否 | 純資料類別 |
| Domain Model / Port | `domain/model/**`、`domain/port/**` | 否 | 介面與值物件 |
| JPA Entity | `adapter/persistence/entity/**` | 否 | 透過 RepositoryImpl 間接驗證 |
| Bootstrap | `bootstrap/**` | 否 | |

> **覆蓋率指標：** JaCoCo **指令覆蓋率（Instruction Coverage）**，80% 門檻。

---

## 3. 測試方法命名

遵循底線分隔的三段模式：

```
{method_or_action}_{scenario}_{expected_outcome}
```

| 錯誤 | 正確 |
|---|---|
| `getLot_happyPath_returnsLotModel` | `get_lot_happyPath_returns_LotModel` |
| `ackAlert_notFound_throwsException` | `ack_alert_not_found_throws_IotCoreException` |
| `processEvent_duplicateKey_skip` | `process_event_duplicate_key_skips_idempotent` |

---

## 4. 各層測試策略

### 4.1 Domain Service

- 使用 **Mockito** 模擬所有 Port / JpaRepository 依賴
- 每個測試方法直接映射到 API 規格中的場景
- 列舉斷言使用列舉同一性，不得與字串比較

```java
@ExtendWith(MockitoExtension.class)
class AlertRuleDomainServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @InjectMocks
    private AlertRuleDomainServiceImpl alertRuleDomainService;

    @Test
    void create_alert_rule_happyPath_returns_AlertRuleModel() { ... }

    @Test
    void create_alert_rule_duplicate_throws_IotCoreException() { ... }

    @Test
    void evaluate_rule_threshold_exceeded_returns_true() { ... }
}
```

### 4.2 UseCase（僅 BFF）

- 透過 Port 介面模擬依賴
- 測試編排邏輯：欄位映射、條件分支、跨服務錯誤傳播
- 測試 RBAC 閘控（Role 驗證）

```java
@ExtendWith(MockitoExtension.class)
class GetLotDetailUseCaseTest {

    @Mock
    private IotcorePort iotcorePort;

    @InjectMocks
    private GetLotDetailUseCaseImpl useCase;

    @Test
    void execute_happyPath_returns_LotDetailRs() { ... }

    @Test
    void execute_lot_not_found_throws_SaasBffException() { ... }

    @Test
    void execute_viewer_role_returns_LotDetailRs() { ... }  // Viewer 可查詢
}
```

### 4.3 Repository（`adapter/persistence`）

- 使用 **Testcontainers PostgreSQL**（業務服務）或 **Testcontainers TimescaleDB**（telemetry-service）
- 測試所有自定義查詢方法（超出標準 JPA CRUD 的部分）
- H2 不適用，因 willThx 使用 PostgreSQL 特有功能（JSONB、TimescaleDB 函數）

```java
@SpringBootTest
@Testcontainers
@EnabledIf(
        value = "org.testcontainers.DockerClientFactory#isDockerAvailable",
        disabledReason = "Docker is not available — skipping container-based test"
)
class LotRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private LotJpaRepository lotJpaRepository;

    @Test
    void find_active_lots_by_tenant_returns_list() { ... }
}
```

### 4.4 TimescaleDB Repository（telemetry-service）

```java
@Container
static GenericContainer<?> timescaledb =
        new GenericContainer<>("timescale/timescaledb:latest-pg16")
                .withExposedPorts(5432)
                .withEnv("POSTGRES_PASSWORD", "test");
```

### 4.5 Feign Client 適配器（saas-bff）

Mock 底層 `*FeignApi`，驗證錯誤映射與 DTO 解包邏輯：

```java
@ExtendWith(MockitoExtension.class)
class IotcoreFeignClientTest {

    @Mock
    private IotcoreFeignApi iotcoreFeignApi;

    @InjectMocks
    private IotcoreFeignClient iotcoreFeignClient;

    @Test
    void get_lot_detail_happyPath_returns_LotDetailDto() {
        when(iotcoreFeignApi.getLotDetail(anyLong(), anyLong(), anyLong()))
                .thenReturn(ApiResponse.success(mockRs()));
        // ...
    }

    @Test
    void get_lot_detail_lot_not_found_throws_SaasBffException() { ... }

    @Test
    void get_lot_detail_feign_exception_throws_SaasBffException() { ... }
}
```

### 4.6 Kafka / RabbitMQ Adapter

使用 Testcontainers，Docker 不可用時自動跳過：

```java
@SpringBootTest
@Testcontainers
@EnabledIf(
        value = "org.testcontainers.DockerClientFactory#isDockerAvailable",
        disabledReason = "Docker is not available — skipping container-based test"
)
class TelemetryKafkaConsumerTest {

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Test
    void consume_telemetry_event_writes_to_timescaledb() { ... }

    @Test
    void consume_duplicate_event_skips_idempotent() { ... }
}
```

### 4.7 Idempotency 測試（iotcore-service）

冪等機制是業務正確性的關鍵，必須測試：

```java
@Test
void process_event_duplicate_key_skips_idempotent() {
    String eventId = "evt-12345";
    // 第一次處理
    domainService.processEvent(eventId, mockEvent());
    // 第二次處理相同 eventId 必須 no-op
    domainService.processEvent(eventId, mockEvent());
    // 驗證只寫入一次
    verify(unitRepository, times(1)).save(any());
}
```

### 4.8 Controller

- 使用 `@SpringBootTest` + `MockMvc`
- 必要：一個 Happy Path（驗證 `ApiResponse.SUCCESS_CODE`）
- 必要：1-2 個錯誤場景（驗證正確的 `responseCode`）

```java
@SpringBootTest
@AutoConfigureMockMvc
class LotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void get_lot_detail_happyPath_returns_success_ApiResponse() throws Exception {
        mockMvc.perform(get("/internal/lots/1")
                .header("X-User-Id", 1L)
                .header("X-Tenant-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(ApiResponse.SUCCESS_CODE));
    }

    @Test
    void get_lot_detail_not_found_returns_IC00004() throws Exception {
        mockMvc.perform(get("/internal/lots/99999")
                .header("X-User-Id", 1L)
                .header("X-Tenant-Id", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.responseCode").value("IC00004"));
    }
}
```

---

## 5. 品質門檻（Quality Gate）

| 指標 | 門檻值 | 強制方式 |
|---|---|---|
| JaCoCo 指令覆蓋率 | >= 80% | 推送前開發者自檢；CI 管線啟用後強制 |
| SonarQube 重大問題（Major Issues） | 0 | CI/CD 管線 |

---

## 6. Maven POM 設定（以 iotcore-service 為例）

```xml
<properties>
    <sonar.coverage.exclusions>
        **/com/willthx/iotcore/bootstrap/**,
        **/com/willthx/iotcore/domain/model/**,
        **/com/willthx/iotcore/domain/port/**,
        **/com/willthx/iotcore/adapter/persistence/entity/**,
        **/*FeignApi.java,
        **/IotCoreApplication.java
    </sonar.coverage.exclusions>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>verify</phase>
                    <goals><goal>report</goal></goals>
                    <configuration>
                        <includes>
                            <include>com/willthx/iotcore/domain/service/**</include>
                            <include>com/willthx/iotcore/adapter/**</include>
                        </includes>
                        <excludes>
                            <exclude>com/willthx/iotcore/adapter/persistence/entity/**</exclude>
                            <exclude>**/*FeignApi.class</exclude>
                        </excludes>
                    </configuration>
                </execution>
                <execution>
                    <id>check</id>
                    <goals><goal>check</goal></goals>
                    <configuration>
                        <includes>
                            <include>com/willthx/iotcore/domain/service/**</include>
                            <include>com/willthx/iotcore/adapter/**</include>
                        </includes>
                        <excludes>
                            <exclude>com/willthx/iotcore/adapter/persistence/entity/**</exclude>
                            <exclude>**/*FeignApi.class</exclude>
                        </excludes>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>INSTRUCTION</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### BFF 模組（saas-bff）

BFF 的 UseCase 位於 `application/usecase/`，includes 需反映：

```xml
<includes>
    <include>com/willthx/saas/application/usecase/**</include>
    <include>com/willthx/saas/domain/service/**</include>
    <include>com/willthx/saas/adapter/**</include>
</includes>
```

### iot-adapter 模組

iot-adapter 以 Adapter 為主體，無 Domain Service：

```xml
<includes>
    <include>com/willthx/adapter/mqtt/**</include>
    <include>com/willthx/adapter/kafka/**</include>
</includes>
```
