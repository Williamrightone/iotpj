package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.uaa.adapter.persistence.entity.RoleFeaturePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleFeaturePermissionJpaRepository extends JpaRepository<RoleFeaturePermissionEntity, Long> {

    List<RoleFeaturePermissionEntity> findByRoleAndTenantId(String role, Long tenantId);

    List<RoleFeaturePermissionEntity> findByTenantId(Long tenantId);

    @Modifying
    @Query("DELETE FROM RoleFeaturePermissionEntity r WHERE r.role = :role AND r.tenantId = :tenantId")
    void deleteByRoleAndTenantId(@Param("role") String role, @Param("tenantId") Long tenantId);

    @Modifying
    @Query("DELETE FROM RoleFeaturePermissionEntity r WHERE r.featureId = :featureId AND r.tenantId = :tenantId")
    void deleteByFeatureIdAndTenantId(@Param("featureId") Long featureId, @Param("tenantId") Long tenantId);
}
