-- MySQL 8 初始化：建立各服務所需的資料庫與使用者
-- 執行者：mysql image 啟動時以 root 自動執行

CREATE USER IF NOT EXISTS 'willthx'@'%' IDENTIFIED BY 'willthx123';

CREATE DATABASE IF NOT EXISTS uaa_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS iotcore_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON uaa_db.*     TO 'willthx'@'%';
GRANT ALL PRIVILEGES ON iotcore_db.* TO 'willthx'@'%';
FLUSH PRIVILEGES;

-- telemetry_db 建立於獨立的 TimescaleDB 實例（port 5433），不在此建立
