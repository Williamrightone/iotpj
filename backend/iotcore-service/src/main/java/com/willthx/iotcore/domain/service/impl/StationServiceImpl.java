package com.willthx.iotcore.domain.service.impl;

import com.willthx.iotcore.domain.model.*;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import com.willthx.iotcore.domain.port.MachineRepository;
import com.willthx.iotcore.domain.port.StationRepository;
import com.willthx.iotcore.domain.service.StationService;
import com.willthx.iotcore.exception.IotCoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.willthx.iotcore.exception.IotCoreException.IotCoreErrorType.*;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {

    private final StationRepository      stationRepository;
    private final MachineRepository      machineRepository;
    private final IotComponentRepository componentRepository;

    @Override
    public List<StationSummaryModel> listStations(Long tenantId) {
        return stationRepository.findAllByTenantId(tenantId).stream()
                .map(s -> StationSummaryModel.builder()
                        .station(s)
                        .activeMachineCount(machineRepository.countActiveByStationId(s.getId()))
                        .activeComponentCount(componentRepository.countActiveByStationId(s.getId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public StationModel createStation(Long tenantId, String stationCode, String name, String description) {
        if (stationRepository.existsByTenantIdAndStationCode(tenantId, stationCode)) {
            throw new IotCoreException(STATION_CODE_DUPLICATE);
        }
        int sortOrder = stationRepository.maxSortOrderByTenantId(tenantId) + 1;
        return stationRepository.save(StationModel.builder()
                .tenantId(tenantId)
                .stationCode(stationCode)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .isActive(true)
                .build());
    }

    @Override
    @Transactional
    public StationModel updateStation(Long id, Long tenantId, String name, String description) {
        StationModel existing = stationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        return stationRepository.save(existing.toBuilder()
                .name(name)
                .description(description)
                .build());
    }

    @Override
    @Transactional
    public void reorderStations(Long tenantId, List<ReorderEntry> orders) {
        for (ReorderEntry entry : orders) {
            stationRepository.findByIdAndTenantId(entry.id(), tenantId)
                    .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        }
        for (ReorderEntry entry : orders) {
            stationRepository.updateSortOrder(entry.id(), tenantId, entry.sortOrder());
        }
    }

    @Override
    @Transactional
    public DeactivateResult deactivateStation(Long id, Long tenantId) {
        StationModel station = stationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        stationRepository.save(station.toBuilder().isActive(false).build());

        List<Long> activeMachineIds = machineRepository.findActiveByStationId(id)
                .stream().map(MachineModel::getId).toList();

        int deactivatedMachines = machineRepository.deactivateAllActiveByStationId(id);

        int deactivatedComponents = 0;
        if (!activeMachineIds.isEmpty()) {
            deactivatedComponents += componentRepository.deactivateAllActiveByMachineIds(activeMachineIds);
        }
        deactivatedComponents += componentRepository.deactivateStationLevelActiveByStationId(id);

        return DeactivateResult.builder()
                .deactivatedMachines(deactivatedMachines)
                .deactivatedComponents(deactivatedComponents)
                .build();
    }

    @Override
    @Transactional
    public StationModel activateStation(Long id, Long tenantId) {
        StationModel station = stationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));
        return stationRepository.save(station.toBuilder().isActive(true).build());
    }

    @Override
    public StationDetailModel getStationDetail(Long id, Long tenantId) {
        StationModel station = stationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IotCoreException(STATION_NOT_FOUND));

        List<ComponentModel> stationComponents = componentRepository.findStationLevelByStationId(id);

        List<MachineDetailModel> machines = machineRepository
                .findAllByStationIdAndTenantId(id, tenantId).stream()
                .map(m -> MachineDetailModel.builder()
                        .machine(m)
                        .components(componentRepository.findAllByMachineId(m.getId()))
                        .build())
                .toList();

        return StationDetailModel.builder()
                .station(station)
                .stationComponents(stationComponents)
                .machines(machines)
                .build();
    }
}
