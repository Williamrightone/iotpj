package com.willthx.iotcore.domain.service;

import com.willthx.iotcore.domain.model.DeactivateResult;
import com.willthx.iotcore.domain.model.MachineModel;

public interface MachineService {

    MachineModel createMachine(Long stationId, Long tenantId, String machineCode, String name, String model);

    MachineModel updateMachine(Long id, Long tenantId, String name, String model);

    DeactivateResult deactivateMachine(Long id, Long tenantId);

    MachineModel activateMachine(Long id, Long tenantId);

    MachineModel copyMachine(Long id, Long tenantId, String newMachineCode, String newName);
}
