package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.adapter.persistence.entity.UserEntity;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.exception.UaaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UserRepository 輸出埠的 JPA Adapter 實作。
 * 負責 Entity ↔ Model 轉換，以及密碼雜湊處理。
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository    userJpaRepository;
    private final PasswordEncoder      passwordEncoder;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public Optional<UserModel> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(this::toModel);
    }

    @Override
    public Optional<UserModel> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    @Transactional
    public UserModel save(UserModel user, String rawPassword) {
        UserEntity entity;
        if (user.getId() == null) {
            // 新增使用者
            if (userJpaRepository.existsByUsername(user.getUsername())) {
                throw new UaaException(UaaException.UaaErrorType.USERNAME_ALREADY_EXISTS);
            }
            entity = new UserEntity();
            entity.setId(snowflakeIdGenerator.nextId());
            entity.setPasswordHash(passwordEncoder.encode(rawPassword));
        } else {
            // 更新使用者
            entity = userJpaRepository.findById(user.getId())
                    .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));
            if (rawPassword != null && !rawPassword.isBlank()) {
                entity.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
        }
        entity.setUsername(user.getUsername());
        entity.setName(user.getName());
        entity.setRole(user.getRole());
        entity.setTenantId(user.getTenantId());
        entity.setActive(user.isActive());
        return toModel(userJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void updateLastLogin(Long userId) {
        userJpaRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    // ── 私有轉換方法 ───────────────────────────────────────────────────────────

    private UserModel toModel(UserEntity entity) {
        return UserModel.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .name(entity.getName())
                .role(entity.getRole())
                .tenantId(entity.getTenantId())
                .active(entity.isActive())
                .lastLoginAt(entity.getLastLoginAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
