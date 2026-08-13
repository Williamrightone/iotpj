package com.willthx.uaa.adapter.persistence.entity;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.persistence.BaseTimeEntity;
import com.willthx.uaa.domain.model.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 使用者 JPA 實體。
 * 不跨越 Adapter 邊界傳遞，由 UserRepositoryImpl 轉換為 UserModel。
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uq_users_account", columnNames = "account")
)
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "account", nullable = false, length = 100)
    private String account;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    /** 所屬租戶；null 表示平台層超級管理員 */
    @Column(name = "tenant_id")
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
