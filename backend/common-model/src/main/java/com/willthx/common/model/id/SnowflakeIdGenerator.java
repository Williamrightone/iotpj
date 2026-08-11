package com.willthx.common.model.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Snowflake ID 生成器。
 *
 * <p>ID 結構（64 bit Long）：
 * <pre>
 * [1 bit 符號] [41 bit 時間戳(ms)] [10 bit Worker ID] [12 bit 序列號]
 * </pre>
 *
 * <p>Worker ID 透過 {@code snowflake.worker-id} 設定（預設 1）。
 * 生產環境（Kubernetes）應搭配 Redis Pod 動態租約，後續版本補充。
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH         = 1_700_000_000_000L; // 2023-11-15
    private static final long WORKER_BITS   = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_BITS) - 1;
    private static final long MAX_SEQUENCE  = (1L << SEQUENCE_BITS) - 1;

    private final long workerId;
    private long  lastTimestamp = -1L;
    private long  sequence      = 0L;

    public SnowflakeIdGenerator(@Value("${snowflake.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "Snowflake worker-id must be between 0 and " + MAX_WORKER_ID + ", got: " + workerId);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp) {
                    Thread.onSpinWait();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << (WORKER_BITS + SEQUENCE_BITS))
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }
}
