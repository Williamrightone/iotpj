package com.willthx.iotcore.domain.service.impl;

import com.willthx.iotcore.domain.model.*;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import com.willthx.iotcore.domain.port.MachineRepository;
import com.willthx.iotcore.domain.port.StationRepository;
import com.willthx.iotcore.exception.IotCoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.willthx.iotcore.exception.IotCoreException.IotCoreErrorType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

    @Mock StationRepository      stationRepo;
    @Mock MachineRepository      machineRepo;
    @Mock IotComponentRepository componentRepo;

    @InjectMocks StationServiceImpl service;

    private StationModel station(Long id, Long tenantId) {
        return StationModel.builder()
                .id(id).tenantId(tenantId).stationCode("ST001").name("Station 1")
                .sortOrder(1).isActive(true).build();
    }

    // ── listStations ────────────────────────────────────────────────────────────

    @Test
    void listStations_returns_summaries_with_counts() {
        StationModel s = station(1L, 10L);
        given(stationRepo.findAllByTenantId(10L)).willReturn(List.of(s));
        given(machineRepo.countActiveByStationId(1L)).willReturn(3);
        given(componentRepo.countActiveByStationId(1L)).willReturn(5);

        List<StationSummaryModel> result = service.listStations(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStation()).isSameAs(s);
        assertThat(result.get(0).getActiveMachineCount()).isEqualTo(3);
        assertThat(result.get(0).getActiveComponentCount()).isEqualTo(5);
    }

    @Test
    void listStations_returns_empty_list_when_no_stations() {
        given(stationRepo.findAllByTenantId(10L)).willReturn(List.of());
        assertThat(service.listStations(10L)).isEmpty();
    }

    // ── createStation ───────────────────────────────────────────────────────────

    @Test
    void createStation_happy_path() {
        given(stationRepo.existsByTenantIdAndStationCode(10L, "ST001")).willReturn(false);
        given(stationRepo.maxSortOrderByTenantId(10L)).willReturn(2);
        StationModel saved = station(1L, 10L);
        given(stationRepo.save(any())).willReturn(saved);

        StationModel result = service.createStation(10L, "ST001", "Station 1", "desc");

        assertThat(result).isSameAs(saved);
        then(stationRepo).should().save(argThat(m ->
                m.getSortOrder() == 3 && m.getIsActive() && "ST001".equals(m.getStationCode())));
    }

    @Test
    void createStation_throws_STATION_CODE_DUPLICATE_when_code_exists() {
        given(stationRepo.existsByTenantIdAndStationCode(10L, "ST001")).willReturn(true);

        assertThatThrownBy(() -> service.createStation(10L, "ST001", "n", null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_CODE_DUPLICATE.getCustomErrorCode());
    }

    // ── updateStation ───────────────────────────────────────────────────────────

    @Test
    void updateStation_happy_path() {
        StationModel existing = station(1L, 10L);
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(existing));
        StationModel updated = existing.toBuilder().name("New Name").build();
        given(stationRepo.save(any())).willReturn(updated);

        StationModel result = service.updateStation(1L, 10L, "New Name", "desc");

        assertThat(result).isSameAs(updated);
    }

    @Test
    void updateStation_throws_STATION_NOT_FOUND_when_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStation(99L, 10L, "n", null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }

    // ── reorderStations ─────────────────────────────────────────────────────────

    @Test
    void reorderStations_happy_path_validates_all_then_updates() {
        StationModel s1 = station(1L, 10L);
        StationModel s2 = station(2L, 10L);
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(s1));
        given(stationRepo.findByIdAndTenantId(2L, 10L)).willReturn(Optional.of(s2));

        List<ReorderEntry> orders = List.of(new ReorderEntry(1L, 2), new ReorderEntry(2L, 1));
        service.reorderStations(10L, orders);

        then(stationRepo).should().updateSortOrder(1L, 10L, 2);
        then(stationRepo).should().updateSortOrder(2L, 10L, 1);
    }

    @Test
    void reorderStations_throws_STATION_NOT_FOUND_on_unknown_id() {
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(station(1L, 10L)));
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.reorderStations(10L,
                List.of(new ReorderEntry(1L, 1), new ReorderEntry(99L, 2))))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());

        then(stationRepo).should(never()).updateSortOrder(anyLong(), anyLong(), anyInt());
    }

    // ── deactivateStation ───────────────────────────────────────────────────────

    @Test
    void deactivateStation_cascades_to_machines_and_components() {
        StationModel s = station(1L, 10L);
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(s));
        MachineModel m1 = MachineModel.builder().id(10L).tenantId(10L).stationId(1L)
                .machineCode("M1").name("M1").isActive(true).build();
        given(machineRepo.findActiveByStationId(1L)).willReturn(List.of(m1));
        given(machineRepo.deactivateAllActiveByStationId(1L)).willReturn(1);
        given(componentRepo.deactivateAllActiveByMachineIds(List.of(10L))).willReturn(3);
        given(componentRepo.deactivateStationLevelActiveByStationId(1L)).willReturn(2);

        DeactivateResult result = service.deactivateStation(1L, 10L);

        assertThat(result.getDeactivatedMachines()).isEqualTo(1);
        assertThat(result.getDeactivatedComponents()).isEqualTo(5);
    }

    @Test
    void deactivateStation_with_no_machines_still_deactivates_station_level_components() {
        StationModel s = station(1L, 10L);
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(s));
        given(machineRepo.findActiveByStationId(1L)).willReturn(List.of());
        given(machineRepo.deactivateAllActiveByStationId(1L)).willReturn(0);
        given(componentRepo.deactivateStationLevelActiveByStationId(1L)).willReturn(4);

        DeactivateResult result = service.deactivateStation(1L, 10L);

        assertThat(result.getDeactivatedMachines()).isEqualTo(0);
        assertThat(result.getDeactivatedComponents()).isEqualTo(4);
        then(componentRepo).should(never()).deactivateAllActiveByMachineIds(any());
    }

    @Test
    void deactivateStation_throws_STATION_NOT_FOUND_when_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateStation(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }

    // ── activateStation ─────────────────────────────────────────────────────────

    @Test
    void activateStation_happy_path() {
        StationModel s = station(1L, 10L).toBuilder().isActive(false).build();
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(s));
        StationModel activated = s.toBuilder().isActive(true).build();
        given(stationRepo.save(any())).willReturn(activated);

        StationModel result = service.activateStation(1L, 10L);

        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void activateStation_throws_STATION_NOT_FOUND_when_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateStation(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }

    // ── getStationDetail ────────────────────────────────────────────────────────

    @Test
    void getStationDetail_assembles_nested_structure() {
        StationModel s = station(1L, 10L);
        given(stationRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(s));
        given(componentRepo.findStationLevelByStationId(1L)).willReturn(List.of());
        MachineModel m = MachineModel.builder().id(20L).tenantId(10L).stationId(1L)
                .machineCode("M1").name("M1").isActive(true).build();
        given(machineRepo.findAllByStationIdAndTenantId(1L, 10L)).willReturn(List.of(m));
        given(componentRepo.findAllByMachineId(20L)).willReturn(List.of());

        StationDetailModel result = service.getStationDetail(1L, 10L);

        assertThat(result.getStation()).isSameAs(s);
        assertThat(result.getMachines()).hasSize(1);
        assertThat(result.getMachines().get(0).getMachine()).isSameAs(m);
    }

    @Test
    void getStationDetail_throws_STATION_NOT_FOUND_when_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStationDetail(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }
}
