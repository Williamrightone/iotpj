package com.willthx.uaa.domain.model;

import com.willthx.common.model.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 使用者領域模型（在 Domain 層流通，不跨越 Adapter 邊界）。
 * 不包含 passwordHash，密碼驗證在 Adapter 層處理。
 */
@Getter
@Builder
public class UserModel {

    private final Long          id;
    private final String        account;
    private final String        displayName;
    private final Role          role;
    private final Long          tenantId;
    private final UserStatus    status;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
