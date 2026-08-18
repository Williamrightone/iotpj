package com.willthx.iotcore.adapter.persistence.impl;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.iotcore.adapter.persistence.entity.StationEntity;
import com.willthx.iotcore.adapter.persistence.jpa.StationJpaRepository;
import com.willthx.iotcore.domain.model.StationModel;
import com.willthx.iotcore.domain.port.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StationRepositoryImpl implements StationRepository {

    private final StationJpaRepository jpa;
    private final SnowflakeIdGenerator snowflake;

    @Override
    public List<StationModel> findAllByTenantId(Long tenantId) {
        return jpa.findAllByTenantIdOrderBySortOrder(tenantId).stream()
                .map(this::toModel).toList();
    }

    @Override
    public Optional<StationModel> findByIdAndTenantId(Long id, Long tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(this::toModel);
    }

    @Override
    public boolean existsByTenantIdAndStationCode(Long tenantId, String stationCode) {
        return jpa.existsByTenantIdAndStationCode(tenantId, stationCode);
    }

    @Override
    @Transactional
    public StationModel save(StationModel m) {
        StationEntity entity;
        if (m.getId() == null) {
            entity = new StationEntity();
            entity.setId(snowflake.nextId());
        } else {
            entity = jpa.findById(m.getId()).orElse(new StationEntity());
            entity.setId(m.getId());
        }
        entity.setTenantId(m.getTenantId());
        entity.setStationCode(m.getStationCode());
        entity.setName(m.getName());
        entity.setDescription(m.getDescription());
        entity.setSortOrder(m.getSortOrder());
        entity.setIsActive(m.getIsActive());
        return toModel(jpa.save(entity));
    }

    @Override
    public int maxSortOrderByTenantId(Long tenantId) {
        return jpa.findMaxSortOrderByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void updateSortOrder(Long id, Long tenantId, int sortOrder) {
        jpa.updateSortOrder(id, tenantId, sortOrder);
    }

    private StationModel toModel(StationEntity e) {
        return StationModel.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .stationCode(e.getStationCode())
                .name(e.getName())
                .description(e.getDescription())
                .sortOrder(e.getSortOrder())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
