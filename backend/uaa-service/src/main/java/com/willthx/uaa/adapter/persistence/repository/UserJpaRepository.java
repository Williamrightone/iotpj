package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.uaa.adapter.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByAccount(String account);

    boolean existsByAccount(String account);

    List<UserEntity> findByTenantId(Long tenantId);

    long countByTenantIdAndStatusAndRoleAndIdNot(Long tenantId, String status, String role, Long excludeId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :now, u.updatedAt = :now WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status = :status, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id AND u.tenantId = :tenantId")
    void updateStatus(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("status") String status);
}
