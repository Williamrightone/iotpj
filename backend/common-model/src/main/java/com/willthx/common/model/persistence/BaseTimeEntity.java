package com.willthx.common.model.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 所有 JPA 實體的時間戳基底類別。
 *
 * <p>規則：
 * <ul>
 *   <li>所有 Entity 必須繼承此類別</li>
 *   <li>不得手動設定 createdAt / updatedAt，由 JPA 生命週期回呼獨佔管理</li>
 *   <li>TimescaleDB 的 telemetry_records 使用 TIMESTAMPTZ time 欄位，不繼承此類別</li>
 * </ul>
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
