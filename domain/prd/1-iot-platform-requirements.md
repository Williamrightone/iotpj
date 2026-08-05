# PRD-001：IoT 工廠監控平台需求文件

> 版本：v0.1
> 日期：2026-08-05
> 狀態：草稿（需求討論中）

---

## 1. 專案目標

| 目標 | 說明 |
|------|------|
| 練習 IoT 協議 | 實作 MQTT 協議的完整收發流程 |
| 高流量高併發處理 | 模擬工廠多裝置同時推送資料的場景 |
| 訊息佇列實戰 | 同時使用 RabbitMQ 與 Kafka，並明確分工 |

---

## 2. 模擬情境：SMT 產線製程

### 2.1 四個製程站

| 站別 | 說明 | 主要 Telemetry | 對 OEE 的貢獻 |
|------|------|---------------|--------------|
| SMT 錫膏印刷 | 印刷錫膏至 PCB | 印刷壓力、錫膏厚度、刮刀速度 | Availability（換料停機）|
| AOI 光學檢測 | 自動光學外觀檢驗 | 檢測結果（pass/fail/缺陷碼） | Quality（良率來源）|
| 回流焊 | 高溫固化錫膏 | 各溫區溫度、傳送速度 | Performance（週期時間）|
| 最終組裝 | 組裝成品 | 鎖附扭力、組裝完成度 | Quality（最終出站良率）|

### 2.2 跨站別廠務環境監控

不屬於任何製程站，持續推送，作為 Kafka 多資料來源的設計場景。

| 資料 | 型態 | 頻率 |
|------|------|------|
| 溫濕度（ESD 敏感區域管控） | telemetry | 每 10–30 秒 |
| 產線電表功耗 | telemetry | 每 10–30 秒 |

---

## 3. IoT 模擬器設計

- **語言**：Python + Bash
- **部署位置**：i3-6100 獨立機器
- **批號（Lot）建立**：支援 Batch 腳本建立批號，指定單元數量，自動生成 unitSerial
- **生產流程模型**：無縫串行
  - 同批號內：前一 unit 離站，後一 unit 立即進站（批號內流水）
  - 跨批號：治具規格不混料，等批號全部完工再換批
  - 範例：
    ```
    Unit-001: [SMT]──▶[AOI]──▶[Reflow]──▶[Assembly]
    Unit-002:      [SMT]──▶[AOI]──▶[Reflow]──▶[Assembly]
    Unit-003:           [SMT]──▶[AOI]──▶[Reflow]──▶[Assembly]
    ```
- **良率模擬**：AOI 站依設定良率隨機產生 defect，所有品質數據皆為模擬值
- **MQTT 傳輸**：每個虛擬裝置有獨立 Client ID，依站別 publish 到對應 topic

---

## 4. 系統架構

### 4.1 資料流

```
[i3-6100 IoT 模擬器]
  └─ MQTT Publish（各站 telemetry / event）
        │
        ▼
[K8s: EMQX MQTT Broker]（2 節點 cluster）
        │
        ▼
[K8s: Spring Boot IoT Gateway]（MQTT Subscriber → 協議轉換）
        ├─▶ Kafka（telemetry / 時序資料流）
        │       └─▶ Kafka Consumer → InfluxDB（時序儲存）
        │       └─▶ Kafka Consumer → OEE Service（OEE 計算）
        └─▶ RabbitMQ（event / alert / 控制指令）
                └─▶ Alert Service → Telegram Bot / Dashboard
```

### 4.2 訊息佇列分工

| 用途 | 選用 |
|------|------|
| Telemetry 資料流（高吞吐、持久化） | Kafka |
| 異常事件 / 告警路由（低延遲、確認機制）| RabbitMQ |
| 設備控制指令 | RabbitMQ |

---

## 5. 中台（Mid-tier）業務邏輯

| 功能 | 說明 |
|------|------|
| OEE 計算 | Availability × Performance × Quality，整合四站 telemetry 與 event |
| 履歷追溯（Genealogy）| 依 unitSerial 查詢該片板在四站的完整紀錄 |
| 異常即時告警分派 | RabbitMQ 消費異常事件，依 severity 路由通知管道 |
| 人員複判一致性分析 | 比對機械判定 vs 人員複判，回饋校正 AOI 誤判率門檻 |

---

## 6. 技術選型

### 6.1 基礎設施

| 元件 | 選型 | 備註 |
|------|------|------|
| K8s | 完整叢集（kubeadm 或 k3s） | 跑在 Proxmox VM 上 |
| MQTT Broker | EMQX 2 節點 cluster | 支援百萬連線級別 |
| Message Queue | Kafka + RabbitMQ | 各有分工，不互相替代 |
| 時序資料庫 | InfluxDB | Telemetry 儲存，Grafana 原生支援 |
| 業務資料庫 | PostgreSQL | 批號、履歷、OEE 結果 |
| 快取 | Redis | 即時設備狀態（最新一筆）|
| 監控 | Grafana + Prometheus | 指標視覺化 |
| 日誌 | Loki + Promtail | 輕量，整合 Grafana |
| GitOps | ArgoCD | K8s 部署管理 |
| 後端 | Spring Boot（Java）| Microservices |
| 前端 | Vue 3 | SaaS Web 平台 |
| 告警 | Telegram Bot + Dashboard | 依 severity 路由 |

### 6.2 硬體規劃

| 機器 | 角色 |
|------|------|
| Mac Mini | DevOps 控制節點（kubectl、Helm、開發）|
| i3-6100 | IoT 模擬器（Python MQTT publisher）|
| R5-3600 / 128GB | 所有服務（Proxmox + K8s + 周邊 VM）|

### 6.3 VM 配置（R5-3600 Proxmox）

| VM | vCPU | RAM | 用途 |
|----|------|-----|------|
| k8s-master | 2C | 8GB | K8s 控制平面 |
| k8s-worker-1 | 6C | 32GB | App Layer（EMQX、Spring Boot、ArgoCD）|
| k8s-worker-2 | 6C | 32GB | Data Layer（InfluxDB、PostgreSQL、Redis）|
| vm-messaging | 4C | 20GB | Kafka + Zookeeper + RabbitMQ |
| vm-monitoring | 4C | 16GB | Grafana + Prometheus + Loki |
| **合計** | **22C** | **108GB** | 留 ~20GB 給 Proxmox host |

### 6.4 K8s Worker 分工

```
worker-1 (role=app)          worker-2 (role=data)
├─ EMQX cluster              ├─ InfluxDB
├─ Spring Boot services      ├─ PostgreSQL
└─ ArgoCD                    └─ Redis
```

透過 Node Label + nodeSelector 強制排程，體現 K8s 節點調度實踐。

---

## 7. 告警通知

| 管道 | 用途 |
|------|------|
| Grafana Dashboard | 即時監控、趨勢圖 |
| Telegram Bot | 嚴重異常推播通知 |

RabbitMQ 依 severity 路由：
- 低：僅寫入 Dashboard
- 高：同時推送 Telegram

---

## 8. 尚未確認事項

- [ ] 微服務拆分方式（服務數量與邊界）
- [ ] MQTT topic 命名結構
- [ ] Kafka topic / partition 策略
- [ ] unitSerial 格式與批號命名規則
- [ ] K8s 安裝方式（kubeadm vs k3s）
- [ ] Vue 3 SaaS 平台功能範圍
