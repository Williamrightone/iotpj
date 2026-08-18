package com.willthx.iotcore.domain.service.impl;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.iotcore.domain.model.ComponentModel;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import com.willthx.iotcore.domain.port.MachineRepository;
import com.willthx.iotcore.domain.port.StationRepository;
import com.willthx.iotcore.domain.service.ComponentService;
import com.willthx.iotcore.exception.IotCoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.willthx.iotcore.exception.IotCoreException.IotCoreErrorType.*;

@Service
@RequiredArgsConstructor
public class ComponentServiceImpl implements ComponentService {

    private final IotComponentRepository componentRepository;
    private final StationRepository      stationRepository;
    private final MachineRepository      machineRepository;

    @Override
    @Transactional
    public ComponentModel createStationComponent(Long stationId, Long tenantId,
                                                 String componentCode, String name,
                                                 ComponentDataType dataType, String unit,
                                                 Integer reportIntervalSec,
                                                 BigDecimal normalUpper, BigDecimal normalLower) {
        stationRepository.findByIdAndTenantId(stationId, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        if (componentRepository.existsByStationIdAndMachineIdIsNullAndComponentCode(stationId, componentCode)) {
            throw new IotCoreException(COMPONENT_CODE_DUPLICATE);
        }
        return componentRepository.save(ComponentModel.builder()
                .tenantId(tenantId)
                .stationId(stationId)
                .machineId(null)
                .componentCode(componentCode)
                .name(name)
                .dataType(dataType)
                .unit(unit)
                .reportIntervalSec(reportIntervalSec)
                .normalUpper(normalUpper)
                .normalLower(normalLower)
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public ComponentModel createMachineComponent(Long machineId, Long tenantId,
                                                 String componentCode, String name,
                                                 ComponentDataType dataType, String unit,
                                                 Integer reportIntervalSec,
                                                 BigDecimal normalUpper, BigDecimal normalLower) {
        var machine = machineRepository.findByIdAndTenantId(machineId, tenantId)
                .orElseThrow(() -> new IotCoreException(MACHINE_NOT_FOUND));
        if (componentRepository.existsByMachineIdAndComponentCode(machineId, componentCode)) {
            throw new IotCoreException(COMPONENT_CODE_DUPLICATE);
        }
        return componentRepository.save(ComponentModel.builder()
                .tenantId(tenantId)
                .stationId(machine.getStationId())
                .machineId(machineId)
                .componentCode(componentCode)
                .name(name)
                .dataType(dataType)
                .unit(unit)
                .reportIntervalSec(reportIntervalSec)
                .normalUpper(normalUpper)
                .normalLower(normalLower)
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public ComponentModel updateComponent(Long id, Long tenantId, String name,
                                          String unit, Integer reportIntervalSec,
                                          BigDecimal normalUpper, BigDecimal normalLower) {
        ComponentModel existing = componentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(COMPONENT_NOT_FOUND));
        return componentRepository.save(existing.toBuilder()
                .name(name)
                .unit(unit)
                .reportIntervalSec(reportIntervalSec)
                .normalUpper(normalUpper)
                .normalLower(normalLower)
                .build());
    }

    @Override
    @Transactional
    public void deactivateComponent(Long id, Long tenantId) {
        ComponentModel existing = componentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(COMPONENT_NOT_FOUND));
        componentRepository.save(existing.toBuilder().isActive(false).build());
    }

    @Override
    @Transactional
    public ComponentModel activateComponent(Long id, Long tenantId) {
        ComponentModel existing = componentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(COMPONENT_NOT_FOUND));
        return componentRepository.save(existing.toBuilder().isActive(true).build());
    }
}
