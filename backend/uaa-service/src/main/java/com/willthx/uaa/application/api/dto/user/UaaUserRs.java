package com.willthx.uaa.application.api.dto.user;

import com.willthx.uaa.domain.model.UserModel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UaaUserRs {
    private Long          userId;
    private String        account;
    private String        displayName;
    private String        role;
    private Long          tenantId;
    private String        status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    public static UaaUserRs from(UserModel m) {
        return UaaUserRs.builder()
                .userId(m.getId())
                .account(m.getAccount())
                .displayName(m.getDisplayName())
                .role(m.getRole().name())
                .tenantId(m.getTenantId())
                .status(m.getStatus().name())
                .lastLoginAt(m.getLastLoginAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
