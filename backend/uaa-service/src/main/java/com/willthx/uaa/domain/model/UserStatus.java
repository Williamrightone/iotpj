package com.willthx.uaa.domain.model;

/**
 * 使用者帳號狀態。
 * 持久化使用 @Enumerated(EnumType.STRING)。
 */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
