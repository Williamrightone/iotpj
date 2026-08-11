package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.uaa.adapter.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA 宣告式 Repository（Adapter 內部使用）。
 * 不對外暴露，僅由 UserRepositoryImpl 呼叫。
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :now, u.updatedAt = :now WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
