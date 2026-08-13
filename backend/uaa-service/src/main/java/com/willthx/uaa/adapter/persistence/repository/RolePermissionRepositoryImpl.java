package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.adapter.persistence.entity.RoleFeaturePermissionEntity;
import com.willthx.uaa.domain.port.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RolePermissionRepositoryImpl implements RolePermissionRepository {

    private final RoleFeaturePermissionJpaRepository jpaRepository;
    private final SnowflakeIdGenerator               snowflakeIdGenerator;

    @Override
    public List<Long> findFeatureIdsByRoleAndTenantId(String role, Long tenantId) {
        return jpaRepository.findByRoleAndTenantId(role, tenantId).stream()
                .map(RoleFeaturePermissionEntity::getFeatureId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Long>> findAllByTenantId(Long tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(
                        RoleFeaturePermissionEntity::getRole,
                        Collectors.mapping(RoleFeaturePermissionEntity::getFeatureId, Collectors.toList())
                ));
    }

    @Override
    @Transactional
    public void replacePermissions(Long tenantId, String role, List<Long> featureIds) {
        jpaRepository.deleteByRoleAndTenantId(role, tenantId);
        if (!featureIds.isEmpty()) {
            List<RoleFeaturePermissionEntity> entities = featureIds.stream().map(fid -> {
                RoleFeaturePermissionEntity e = new RoleFeaturePermissionEntity();
                e.setId(snowflakeIdGenerator.nextId());
                e.setTenantId(tenantId);
                e.setRole(role);
                e.setFeatureId(fid);
                return e;
            }).collect(Collectors.toList());
            jpaRepository.saveAll(entities);
        }
    }

    @Override
    @Transactional
    public void deleteByFeatureIdAndTenantId(Long featureId, Long tenantId) {
        jpaRepository.deleteByFeatureIdAndTenantId(featureId, tenantId);
    }
}
