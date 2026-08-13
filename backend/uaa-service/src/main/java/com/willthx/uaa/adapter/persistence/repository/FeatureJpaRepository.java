package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.uaa.adapter.persistence.entity.FeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeatureJpaRepository extends JpaRepository<FeatureEntity, Long> {

    List<FeatureEntity> findByTenantId(Long tenantId);

    List<FeatureEntity> findByTenantIdAndActiveTrue(Long tenantId);

    Optional<FeatureEntity> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByFeatureCodeAndTenantId(String featureCode, Long tenantId);

    boolean existsByParentIdAndTenantId(Long parentId, Long tenantId);

    List<FeatureEntity> findByIdInAndTenantId(List<Long> ids, Long tenantId);

    @Modifying
    @Query("UPDATE FeatureEntity f SET f.active = :active, f.updatedAt = CURRENT_TIMESTAMP WHERE f.id = :id AND f.tenantId = :tenantId")
    void updateActive(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("active") boolean active);

    @Modifying
    @Query("DELETE FROM FeatureEntity f WHERE f.id = :id AND f.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
