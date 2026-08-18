package com.willthx.iotcore.domain.service.impl;

import com.willthx.iotcore.domain.model.ComponentModel;
import com.willthx.iotcore.domain.model.DeactivateResult;
import com.willthx.iotcore.domain.model.MachineModel;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MachineServiceImplTest {

    @Mock MachineRepository      machineRepo;
    @Mock StationRepository      stationRepo;
    @Mock IotComponentRepository componentRepo;

    @InjectMocks MachineServiceImpl service;

    private MachineModel machine(Long id, Long stationId, Long tenantId) {
        return MachineModel.builder()
                .id(id).tenantId(tenantId).stationId(stationId)
                .machineCode("M001").name("Machine 1").model("ModelX").isActive(true).build();
    }

    // ── createMachine ────────────────────────────────────────────────────────────

    @Test
    void createMachine_happy_path() {
        given(stationRepo.findByIdAndTenantId(5L, 10L)).willReturn(
                Optional.of(com.willthx.iotcore.domain.model.StationModel.builder()
                        .id(5L).tenantId(10L).stationCode("ST1").name("S1").isActive(true).build()));
        given(machineRepo.existsByTenantIdAndMachineCode(10L, "M001")).willReturn(false);
        MachineModel saved = machine(1L, 5L, 10L);
        given(machineRepo.save(any())).willReturn(saved);

        MachineModel result = service.createMachine(5L, 10L, "M001", "Machine 1", "ModelX");

        assertThat(result).isSameAs(saved);
    }

    @Test
    void createMachine_throws_STATION_NOT_FOUND_when_station_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMachine(99L, 10L, "M001", "M1", null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void createMachine_throws_MACHINE_CODE_DUPLICATE_when_code_exists() {
        given(stationRepo.findByIdAndTenantId(5L, 10L)).willReturn(
                Optional.of(com.willthx.iotcore.domain.model.StationModel.builder()
                        .id(5L).tenantId(10L).stationCode("ST1").name("S1").isActive(true).build()));
        given(machineRepo.existsByTenantIdAndMachineCode(10L, "M001")).willReturn(true);

        assertThatThrownBy(() -> service.createMachine(5L, 10L, "M001", "M1", null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_CODE_DUPLICATE.getCustomErrorCode());
    }

    // ── updateMachine ────────────────────────────────────────────────────────────

    @Test
    void updateMachine_happy_path() {
        MachineModel existing = machine(1L, 5L, 10L);
        given(machineRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(existing));
        MachineModel updated = existing.toBuilder().name("New Name").build();
        given(machineRepo.save(any())).willReturn(updated);

        MachineModel result = service.updateMachine(1L, 10L, "New Name", "NewModel");

        assertThat(result).isSameAs(updated);
    }

    @Test
    void updateMachine_throws_MACHINE_NOT_FOUND_when_missing() {
        given(machineRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMachine(99L, 10L, "n", null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_NOT_FOUND.getCustomErrorCode());
    }

    // ── deactivateMachine ────────────────────────────────────────────────────────

    @Test
    void deactivateMachine_deactivates_machine_and_components() {
        MachineModel m = machine(1L, 5L, 10L);
        given(machineRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(m));
        given(componentRepo.deactivateAllActiveByMachineId(1L)).willReturn(4);

        DeactivateResult result = service.deactivateMachine(1L, 10L);

        assertThat(result.getDeactivatedMachines()).isEqualTo(1);
        assertThat(result.getDeactivatedComponents()).isEqualTo(4);
        then(machineRepo).should().save(argThat(saved -> !saved.getIsActive()));
    }

    @Test
    void deactivateMachine_throws_MACHINE_NOT_FOUND_when_missing() {
        given(machineRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateMachine(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_NOT_FOUND.getCustomErrorCode());
    }

    // ── activateMachine ──────────────────────────────────────────────────────────

    @Test
    void activateMachine_happy_path() {
        MachineModel m = machine(1L, 5L, 10L).toBuilder().isActive(false).build();
        given(machineRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(m));
        MachineModel activated = m.toBuilder().isActive(true).build();
        given(machineRepo.save(any())).willReturn(activated);

        MachineModel result = service.activateMachine(1L, 10L);

        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void activateMachine_throws_MACHINE_NOT_FOUND_when_missing() {
        given(machineRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateMachine(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_NOT_FOUND.getCustomErrorCode());
    }

    // ── copyMachine ──────────────────────────────────────────────────────────────

    @Test
    void copyMachine_copies_machine_and_active_components() {
        MachineModel source = machine(1L, 5L, 10L);
        given(machineRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(source));
        given(machineRepo.existsByTenantIdAndMachineCode(10L, "M002")).willReturn(false);
        MachineModel newMachine = machine(2L, 5L, 10L).toBuilder().machineCode("M002").build();
        given(machineRepo.save(any())).willReturn(newMachine);

        ComponentModel c = ComponentModel.builder()
                .id(100L).tenantId(10L).stationId(5L).machineId(1L)
                .componentCode("C001").name("Comp1")
                .isActive(true).build();
        given(componentRepo.findActiveByMachineId(1L)).willReturn(List.of(c));

        MachineModel result = service.copyMachine(1L, 10L, "M002", "Machine 2");

        assertThat(result).isSameAs(newMachine);
        then(componentRepo).should().saveAll(argThat(list ->
                list.size() == 1 && ((List<ComponentModel>) list).get(0).getMachineId().equals(2L)));
    }

    @Test
    void copyMachine_throws_MACHINE_NOT_FOUND_when_source_missing() {
        given(machineRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.copyMachine(99L, 10L, "M002", "M2"))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void copyMachine_throws_MACHINE_CODE_DUPLICATE_when_code_exists() {
        given(machineRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(machine(1L, 5L, 10L)));
        given(machineRepo.existsByTenantIdAndMachineCode(10L, "M002")).willReturn(true);

        assertThatThrownBy(() -> service.copyMachine(1L, 10L, "M002", "M2"))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_CODE_DUPLICATE.getCustomErrorCode());
    }
}
