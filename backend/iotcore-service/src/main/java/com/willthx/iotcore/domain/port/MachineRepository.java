package com.willthx.iotcore.domain.port;

import com.willthx.iotcore.domain.model.MachineModel;

import java.util.List;
import java.util.Optional;

public interface MachineRepository {

    List<MachineModel> findAllByStationIdAndTenantId(Long stationId, Long tenantId);

    Optional<MachineModel> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndMachineCode(Long tenantId, String machineCode);

    MachineModel save(MachineModel machine);

    List<MachineModel> findActiveByStationId(Long stationId);

    int countActiveByStationId(Long stationId);

    int deactivateAllActiveByStationId(Long stationId);
}
