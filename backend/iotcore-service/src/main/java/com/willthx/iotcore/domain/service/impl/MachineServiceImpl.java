package com.willthx.iotcore.domain.service.impl;

import com.willthx.iotcore.domain.model.ComponentModel;
import com.willthx.iotcore.domain.model.DeactivateResult;
import com.willthx.iotcore.domain.model.MachineModel;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import com.willthx.iotcore.domain.port.MachineRepository;
import com.willthx.iotcore.domain.port.StationRepository;
import com.willthx.iotcore.domain.service.MachineService;
import com.willthx.iotcore.exception.IotCoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.willthx.iotcore.exception.IotCoreException.IotCoreErrorType.*;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineRepository      machineRepository;
    private final StationRepository      stationRepository;
    private final IotComponentRepository componentRepository;

    @Override
    @Transactional
    public MachineModel createMachine(Long stationId, Long tenantId,
                                      String machineCode, String name, String model) {
        stationRepository.findByIdAndTenantId(stationId, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        if (machineRepository.existsByTenantIdAndMachineCode(tenantId, machineCode)) {
            throw new IotCoreException(MACHINE_CODE_DUPLICATE);
        }
        return machineRepository.save(MachineModel.builder()
                .tenantId(tenantId)
                .stationId(stationId)
                .machineCode(machineCode)
                .name(name)
                .model(model)
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public MachineModel updateMachine(Long id, Long tenantId, String name, String model) {
        MachineModel existing = machineRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(MACHINE_NOT_FOUND));
        return machineRepository.save(existing.toBuilder()
                .name(name)
                .model(model)
                .build());
    }

    @Override
    @Transactional
    public DeactivateResult deactivateMachine(Long id, Long tenantId) {
        MachineModel machine = machineRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(MACHINE_NOT_FOUND));
        machineRepository.save(machine.toBuilder().isActive(false).build());
        int deactivatedComponents = componentRepository.deactivateAllActiveByMachineId(id);
        return DeactivateResult.builder()
                .deactivatedMachines(1)
                .deactivatedComponents(deactivatedComponents)
                .build();
    }

    @Override
    @Transactional
    public MachineModel activateMachine(Long id, Long tenantId) {
        MachineModel machine = machineRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(MACHINE_NOT_FOUND));
        return machineRepository.save(machine.toBuilder().isActive(true).build());
    }

    @Override
    @Transactional
    public MachineModel copyMachine(Long id, Long tenantId, String newMachineCode, String newName) {
        MachineModel source = machineRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(MACHINE_NOT_FOUND));
        if (machineRepository.existsByTenantIdAndMachineCode(tenantId, newMachineCode)) {
            throw new IotCoreException(MACHINE_CODE_DUPLICATE);
        }
        MachineModel newMachine = machineRepository.save(MachineModel.builder()
                .tenantId(tenantId)
                .stationId(source.getStationId())
                .machineCode(newMachineCode)
                .name(newName)
                .model(source.getModel())
                .isActive(true)
                .build());

        List<ComponentModel> copies = componentRepository.findActiveByMachineId(id).stream()
                .map(c -> ComponentModel.builder()
                        .tenantId(c.getTenantId())
                        .stationId(c.getStationId())
                        .machineId(newMachine.getId())
                        .componentCode(c.getComponentCode())
                        .name(c.getName())
                        .dataType(c.getDataType())
                        .unit(c.getUnit())
                        .reportIntervalSec(c.getReportIntervalSec())
                        .normalUpper(c.getNormalUpper())
                        .normalLower(c.getNormalLower())
                        .isActive(true)
                        .build())
                .toList();
        componentRepository.saveAll(copies);

        return newMachine;
    }
}
