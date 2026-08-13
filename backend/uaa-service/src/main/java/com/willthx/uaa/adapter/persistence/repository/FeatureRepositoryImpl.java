package com.willthx.uaa.adapter.persistence.repository;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.uaa.adapter.persistence.entity.FeatureEntity;
import com.willthx.uaa.domain.model.FeatureModel;
import com.willthx.uaa.domain.port.FeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FeatureRepositoryImpl implements FeatureRepository {

    private final FeatureJpaRepository featureJpaRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public List<FeatureModel> findAllByTenantId(Long tenantId) {
        return featureJpaRepository.findByTenantId(tenantId).stream()
                .map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public List<FeatureModel> findActiveByTenantId(Long tenantId) {
        return featureJpaRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public Optional<FeatureModel> findByIdAndTenantId(Long id, Long tenantId) {
        return featureJpaRepository.findByIdAndTenantId(id, tenantId).map(this::toModel);
    }

    @Override
    public List<FeatureModel> findByIdsAndTenantId(List<Long> ids, Long tenantId) {
        if (ids.isEmpty()) return List.of();
        return featureJpaRepository.findByIdInAndTenantId(ids, tenantId).stream()
                .map(this::toModel).collect(Collectors.toList());
    }

    @Override
    public boolean existsByCodeAndTenantId(String featureCode, Long tenantId) {
        return featureJpaRepository.existsByFeatureCodeAndTenantId(featureCode, tenantId);
    }

    @Override
    public boolean hasChildrenByParentId(Long parentId, Long tenantId) {
        return featureJpaRepository.existsByParentIdAndTenantId(parentId, tenantId);
    }

    @Override
    @Transactional
    public FeatureModel save(FeatureModel feature) {
        FeatureEntity entity;
        if (featureJpaRepository.existsById(feature.getId())) {
            entity = featureJpaRepository.findById(feature.getId()).orElseThrow();
        } else {
            entity = new FeatureEntity();
            entity.setId(feature.getId() != null ? feature.getId() : snowflakeIdGenerator.nextId());
        }
        entity.setTenantId(feature.getTenantId());
        entity.setParentId(feature.getParentId());
        entity.setFeatureCode(feature.getFeatureCode());
        entity.setFeatureName(feature.getFeatureName());
        entity.setRoute(feature.getRoute());
        entity.setSortOrder(feature.getSortOrder());
        entity.setActive(feature.isActive());
        return toModel(featureJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void setActive(Long id, Long tenantId, boolean active) {
        featureJpaRepository.updateActive(id, tenantId, active);
    }

    @Override
    @Transactional
    public void deleteByIdAndTenantId(Long id, Long tenantId) {
        featureJpaRepository.deleteByIdAndTenantId(id, tenantId);
    }

    private FeatureModel toModel(FeatureEntity e) {
        return FeatureModel.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .parentId(e.getParentId())
                .featureCode(e.getFeatureCode())
                .featureName(e.getFeatureName())
                .route(e.getRoute())
                .sortOrder(e.getSortOrder())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
