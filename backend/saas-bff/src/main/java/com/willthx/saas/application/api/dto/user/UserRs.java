package com.willthx.saas.application.api.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRs {
    private Long          userId;
    private String        account;
    private String        displayName;
    private String        role;
    private Long          tenantId;
    private String        status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
