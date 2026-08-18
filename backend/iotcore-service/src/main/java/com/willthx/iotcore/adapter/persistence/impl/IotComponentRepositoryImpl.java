package com.willthx.iotcore.adapter.persistence.impl;

import com.willthx.common.model.id.SnowflakeIdGenerator;
import com.willthx.iotcore.adapter.persistence.entity.IotComponentEntity;
import com.willthx.iotcore.adapter.persistence.jpa.IotComponentJpaRepository;
import com.willthx.iotcore.domain.model.ComponentModel;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IotComponentRepositoryImpl implements IotComponentRepository {

    private final IotComponentJpaRepository jpa;
    private final SnowflakeIdGenerator      snowflake;

    @Override
    public List<ComponentModel> findAllByMachineId(Long machineId) {
        return jpa.findAllByMachineId(machineId).stream().map(this::toModel).toList();
    }

    @Override
    public List<ComponentModel> findStationLevelByStationId(Long stationId) {
        return jpa.findAllByStationIdAndMachineIdIsNull(stationId).stream().map(this::toModel).toList();
    }

    @Override
    public Optional<ComponentModel> findByIdAndTenantId(Long id, Long tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId).map(this::toModel);
    }

    @Override
    public boolean existsByMachineIdAndComponentCode(Long machineId, String componentCode) {
        return jpa.existsByMachineIdAndComponentCode(machineId, componentCode);
    }

    @Override
    public boolean existsByStationIdAndMachineIdIsNullAndComponentCode(Long stationId, String componentCode) {
        return jpa.existsByStationIdAndMachineIdIsNullAndComponentCode(stationId, componentCode);
    }

    @Override
    @Transactional
    public ComponentModel save(ComponentModel m) {
        IotComponentEntity entity;
        if (m.getId() == null) {
            entity = new IotComponentEntity();
            entity.setId(snowflake.nextId());
        } else {
            entity = jpa.findById(m.getId()).orElse(new IotComponentEntity());
            entity.setId(m.getId());
        }
        entity.setTenantId(m.getTenantId());
        entity.setStationId(m.getStationId());
        entity.setMachineId(m.getMachineId());
        entity.setComponentCode(m.getComponentCode());
        entity.setName(m.getName());
        entity.setDataType(m.getDataType());
        entity.setUnit(m.getUnit());
        entity.setReportIntervalSec(m.getReportIntervalSec());
        entity.setNormalUpper(m.getNormalUpper());
        entity.setNormalLower(m.getNormalLower());
        entity.setIsActive(m.getIsActive());
        return toModel(jpa.save(entity));
    }

    @Override
    @Transactional
    public void saveAll(List<ComponentModel> components) {
        List<IotComponentEntity> entities = components.stream().map(m -> {
            IotComponentEntity e = new IotComponentEntity();
            e.setId(snowflake.nextId());
            e.setTenantId(m.getTenantId());
            e.setStationId(m.getStationId());
            e.setMachineId(m.getMachineId());
            e.setComponentCode(m.getComponentCode());
            e.setName(m.getName());
            e.setDataType(m.getDataType());
            e.setUnit(m.getUnit());
            e.setReportIntervalSec(m.getReportIntervalSec());
            e.setNormalUpper(m.getNormalUpper());
            e.setNormalLower(m.getNormalLower());
            e.setIsActive(m.getIsActive());
            return e;
        }).toList();
        jpa.saveAll(entities);
    }

    @Override
    public int countActiveByStationId(Long stationId) {
        return jpa.countByStationIdAndIsActiveTrue(stationId);
    }

    @Override
    @Transactional
    public int deactivateAllActiveByMachineId(Long machineId) {
        return jpa.deactivateActiveByMachineId(machineId);
    }

    @Override
    @Transactional
    public int deactivateAllActiveByMachineIds(List<Long> machineIds) {
        if (machineIds.isEmpty()) return 0;
        return jpa.deactivateActiveByMachineIds(machineIds);
    }

    @Override
    @Transactional
    public int deactivateStationLevelActiveByStationId(Long stationId) {
        return jpa.deactivateStationLevelActiveByStationId(stationId);
    }

    @Override
    public List<ComponentModel> findActiveByMachineId(Long machineId) {
        return jpa.findByMachineIdAndIsActiveTrue(machineId).stream().map(this::toModel).toList();
    }

    private ComponentModel toModel(IotComponentEntity e) {
        return ComponentModel.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .stationId(e.getStationId())
                .machineId(e.getMachineId())
                .componentCode(e.getComponentCode())
                .name(e.getName())
                .dataType(e.getDataType())
                .unit(e.getUnit())
                .reportIntervalSec(e.getReportIntervalSec())
                .normalUpper(e.getNormalUpper())
                .normalLower(e.getNormalLower())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
