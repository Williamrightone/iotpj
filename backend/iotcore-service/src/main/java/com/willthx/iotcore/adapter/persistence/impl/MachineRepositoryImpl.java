package com.willthx.iotcore.adapter.persistence.impl;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.iotcore.adapter.persistence.entity.MachineEntity;
import com.willthx.iotcore.adapter.persistence.jpa.MachineJpaRepository;
import com.willthx.iotcore.domain.model.MachineModel;
import com.willthx.iotcore.domain.port.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MachineRepositoryImpl implements MachineRepository {

    private final MachineJpaRepository jpa;
    private final SnowflakeIdGenerator snowflake;

    @Override
    public List<MachineModel> findAllByStationIdAndTenantId(Long stationId, Long tenantId) {
        return jpa.findAllByStationIdAndTenantId(stationId, tenantId).stream()
                .map(this::toModel).toList();
    }

    @Override
    public Optional<MachineModel> findByIdAndTenantId(Long id, Long tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(this::toModel);
    }

    @Override
    public boolean existsByTenantIdAndMachineCode(Long tenantId, String machineCode) {
        return jpa.existsByTenantIdAndMachineCode(tenantId, machineCode);
    }

    @Override
    @Transactional
    public MachineModel save(MachineModel m) {
        MachineEntity entity;
        if (m.getId() == null) {
            entity = new MachineEntity();
            entity.setId(snowflake.nextId());
        } else {
            entity = jpa.findById(m.getId()).orElse(new MachineEntity());
            entity.setId(m.getId());
        }
        entity.setTenantId(m.getTenantId());
        entity.setStationId(m.getStationId());
        entity.setMachineCode(m.getMachineCode());
        entity.setName(m.getName());
        entity.setModel(m.getModel());
        entity.setIsActive(m.getIsActive());
        return toModel(jpa.save(entity));
    }

    @Override
    public List<MachineModel> findActiveByStationId(Long stationId) {
        return jpa.findByStationIdAndIsActiveTrue(stationId).stream()
                .map(this::toModel).toList();
    }

    @Override
    public int countActiveByStationId(Long stationId) {
        return jpa.countByStationIdAndIsActiveTrue(stationId);
    }

    @Override
    @Transactional
    public int deactivateAllActiveByStationId(Long stationId) {
        return jpa.deactivateActiveByStationId(stationId);
    }

    private MachineModel toModel(MachineEntity e) {
        return MachineModel.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .stationId(e.getStationId())
                .machineCode(e.getMachineCode())
                .name(e.getName())
                .model(e.getModel())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
