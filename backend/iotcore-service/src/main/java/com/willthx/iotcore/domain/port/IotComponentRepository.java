package com.willthx.iotcore.domain.port;

import com.willthx.iotcore.domain.model.ComponentModel;

import java.util.List;
import java.util.Optional;

public interface IotComponentRepository {

    List<ComponentModel> findAllByMachineId(Long machineId);

    List<ComponentModel> findStationLevelByStationId(Long stationId);

    Optional<ComponentModel> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByMachineIdAndComponentCode(Long machineId, String componentCode);

    boolean existsByStationIdAndMachineIdIsNullAndComponentCode(Long stationId, String componentCode);

    ComponentModel save(ComponentModel component);

    void saveAll(List<ComponentModel> components);

    int countActiveByStationId(Long stationId);

    int deactivateAllActiveByMachineId(Long machineId);

    int deactivateAllActiveByMachineIds(List<Long> machineIds);

    int deactivateStationLevelActiveByStationId(Long stationId);

    List<ComponentModel> findActiveByMachineId(Long machineId);
}
