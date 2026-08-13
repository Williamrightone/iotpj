package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.domain.model.FeatureModel;
import com.willthx.uaa.domain.port.FeatureRepository;
import com.willthx.uaa.domain.port.RolePermissionRepository;
import com.willthx.uaa.domain.service.FeatureService;
import com.willthx.uaa.exception.UaaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository        featureRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SnowflakeIdGenerator     snowflakeIdGenerator;

    @Override
    public List<FeatureModel> listFeatures(Long tenantId) {
        return featureRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional
    public FeatureModel createFeature(Long tenantId, Long parentId, String featureCode,
                                      String featureName, String route, int sortOrder) {
        if (featureRepository.existsByCodeAndTenantId(featureCode, tenantId)) {
            throw new UaaException(UaaException.UaaErrorType.FEATURE_CODE_DUPLICATE);
        }
        if (parentId != null) {
            featureRepository.findByIdAndTenantId(parentId, tenantId)
                    .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.FEATURE_NOT_FOUND));
        }

        FeatureModel feature = FeatureModel.builder()
                .id(snowflakeIdGenerator.nextId())
                .tenantId(tenantId)
                .parentId(parentId)
                .featureCode(featureCode)
                .featureName(featureName)
                .route(route)
                .sortOrder(sortOrder)
                .active(true)
                .build();
        return featureRepository.save(feature);
    }

    @Override
    @Transactional
    public FeatureModel updateFeature(Long id, Long tenantId, String featureName,
                                      String route, int sortOrder) {
        FeatureModel existing = featureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.FEATURE_NOT_FOUND));

        FeatureModel updated = FeatureModel.builder()
                .id(existing.getId())
                .tenantId(existing.getTenantId())
                .parentId(existing.getParentId())
                .featureCode(existing.getFeatureCode())
                .featureName(featureName)
                .route(route)
                .sortOrder(sortOrder)
                .active(existing.isActive())
                .build();
        return featureRepository.save(updated);
    }

    @Override
    @Transactional
    public void setActive(Long id, Long tenantId, boolean active) {
        featureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.FEATURE_NOT_FOUND));
        featureRepository.setActive(id, tenantId, active);
    }

    @Override
    @Transactional
    public void deleteFeature(Long id, Long tenantId) {
        FeatureModel feature = featureRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.FEATURE_NOT_FOUND));

        if (feature.getParentId() == null) {
            // 父功能：確認無子功能
            if (featureRepository.hasChildrenByParentId(id, tenantId)) {
                throw new UaaException(UaaException.UaaErrorType.PARENT_HAS_CHILDREN);
            }
        } else {
            // 子功能：同步清除 role_feature_permissions
            rolePermissionRepository.deleteByFeatureIdAndTenantId(id, tenantId);
        }
        featureRepository.deleteByIdAndTenantId(id, tenantId);
    }

    @Override
    public Map<String, Object> getRolePermissions(Long tenantId) {
        List<FeatureModel> leaves = featureRepository.findAllByTenantId(tenantId).stream()
                .filter(f -> f.getParentId() != null)
                .collect(Collectors.toList());

        Map<String, List<Long>> permissions = rolePermissionRepository.findAllByTenantId(tenantId);
        permissions.putIfAbsent("MAINTAINER", List.of());
        permissions.putIfAbsent("VIEWER", List.of());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("features",    leaves);
        result.put("permissions", permissions);
        return result;
    }

    @Override
    @Transactional
    public void updateRolePermissions(Long tenantId, String role, List<Long> featureIds) {
        if (!featureIds.isEmpty()) {
            List<FeatureModel> found = featureRepository.findByIdsAndTenantId(featureIds, tenantId);
            if (found.size() != featureIds.size()) {
                throw new UaaException(UaaException.UaaErrorType.FEATURE_ID_INVALID);
            }
            boolean hasParent = found.stream().anyMatch(f -> f.getParentId() == null);
            if (hasParent) {
                throw new UaaException(UaaException.UaaErrorType.PERMISSION_LEAF_ONLY);
            }
        }
        rolePermissionRepository.replacePermissions(tenantId, role, featureIds);
    }

    @Override
    public List<FeatureModel> getFeatureTree(Long tenantId, Role role) {
        if (role == Role.ADMIN) {
            return featureRepository.findActiveByTenantId(tenantId).stream()
                    .sorted(Comparator.comparingInt(FeatureModel::getSortOrder))
                    .collect(Collectors.toList());
        }

        // 非 Admin：先查子功能 ID，再補父功能
        List<Long> leafIds = rolePermissionRepository.findFeatureIdsByRoleAndTenantId(role.name(), tenantId);
        if (leafIds.isEmpty()) return List.of();

        List<FeatureModel> leaves = featureRepository.findByIdsAndTenantId(leafIds, tenantId).stream()
                .filter(FeatureModel::isActive)
                .collect(Collectors.toList());

        Set<Long> parentIds = leaves.stream()
                .map(FeatureModel::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<FeatureModel> parents = featureRepository.findByIdsAndTenantId(new ArrayList<>(parentIds), tenantId).stream()
                .filter(FeatureModel::isActive)
                .collect(Collectors.toList());

        List<FeatureModel> result = new ArrayList<>(parents);
        result.addAll(leaves);
        result.sort(Comparator.comparingInt(FeatureModel::getSortOrder));
        return result;
    }
}
