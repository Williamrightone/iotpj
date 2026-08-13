package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.adapter.persistence.entity.UserEntity;
import com.willthx.uaa.adapter.persistence.entity.UserStationBindingEntity;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.exception.UaaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository               userJpaRepository;
    private final UserStationBindingJpaRepository stationJpaRepository;
    private final PasswordEncoder                 passwordEncoder;
    private final SnowflakeIdGenerator            snowflakeIdGenerator;

    @Override
    public Optional<UserModel> authenticate(String account, String rawPassword) {
        return userJpaRepository.findByAccount(account)
                .filter(e -> passwordEncoder.matches(rawPassword, e.getPasswordHash()))
                .map(this::toModel);
    }

    @Override
    public Optional<UserModel> findByAccount(String account) {
        return userJpaRepository.findByAccount(account).map(this::toModel);
    }

    @Override
    public Optional<UserModel> findById(Long id) {
        return userJpaRepository.findById(id).map(this::toModel);
    }

    @Override
    public List<UserModel> findAllByTenantId(Long tenantId) {
        return userJpaRepository.findByTenantId(tenantId).stream()
                .map(this::toModel).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserModel save(UserModel user, String rawPassword) {
        UserEntity entity;
        if (user.getId() == null) {
            if (userJpaRepository.existsByAccount(user.getAccount())) {
                throw new UaaException(UaaException.UaaErrorType.ACCOUNT_ALREADY_EXISTS);
            }
            entity = new UserEntity();
            entity.setId(snowflakeIdGenerator.nextId());
            entity.setPasswordHash(passwordEncoder.encode(rawPassword));
        } else {
            entity = userJpaRepository.findById(user.getId())
                    .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));
            if (rawPassword != null && !rawPassword.isBlank()) {
                entity.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
        }
        entity.setAccount(user.getAccount());
        entity.setDisplayName(user.getDisplayName());
        entity.setRole(user.getRole());
        entity.setTenantId(user.getTenantId());
        entity.setStatus(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE);
        return toModel(userJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void updateLastLogin(Long userId) {
        userJpaRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Long tenantId, UserStatus status) {
        userJpaRepository.updateStatus(id, tenantId, status.name());
    }

    @Override
    public long countActiveAdminsExcluding(Long tenantId, Long excludeUserId) {
        return userJpaRepository.countByTenantIdAndStatusAndRoleAndIdNot(
                tenantId, UserStatus.ACTIVE.name(), Role.ADMIN.name(), excludeUserId);
    }

    // ── 站點綁定 ──────────────────────────────────────────────────────────────

    @Override
    public List<String> findStationIds(Long userId, Long tenantId) {
        return stationJpaRepository.findByUserIdAndTenantId(userId, tenantId).stream()
                .map(UserStationBindingEntity::getStationId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void replaceStations(Long userId, Long tenantId, List<String> stationIds) {
        stationJpaRepository.deleteByUserIdAndTenantId(userId, tenantId);
        if (stationIds != null && !stationIds.isEmpty()) {
            List<UserStationBindingEntity> entities = stationIds.stream().map(sid -> {
                UserStationBindingEntity e = new UserStationBindingEntity();
                e.setId(snowflakeIdGenerator.nextId());
                e.setUserId(userId);
                e.setStationId(sid);
                e.setTenantId(tenantId);
                return e;
            }).collect(Collectors.toList());
            stationJpaRepository.saveAll(entities);
        }
    }

    // ── 轉換 ─────────────────────────────────────────────────────────────────

    private UserModel toModel(UserEntity e) {
        return UserModel.builder()
                .id(e.getId())
                .account(e.getAccount())
                .displayName(e.getDisplayName())
                .role(e.getRole())
                .tenantId(e.getTenantId())
                .status(e.getStatus())
                .lastLoginAt(e.getLastLoginAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
