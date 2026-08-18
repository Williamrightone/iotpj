package com.willthx.iotcore.domain.service.impl;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.iotcore.domain.model.ComponentModel;
import com.willthx.iotcore.domain.model.MachineModel;
import com.willthx.iotcore.domain.model.StationModel;
import com.willthx.iotcore.domain.port.IotComponentRepository;
import com.willthx.iotcore.domain.port.MachineRepository;
import com.willthx.iotcore.domain.port.StationRepository;
import com.willthx.iotcore.exception.IotCoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.willthx.iotcore.exception.IotCoreException.IotCoreErrorType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ComponentServiceImplTest {

    @Mock IotComponentRepository componentRepo;
    @Mock StationRepository      stationRepo;
    @Mock MachineRepository      machineRepo;

    @InjectMocks ComponentServiceImpl service;

    private StationModel station(Long id, Long tenantId) {
        return StationModel.builder().id(id).tenantId(tenantId)
                .stationCode("ST1").name("S1").isActive(true).build();
    }

    private MachineModel machine(Long id, Long stationId, Long tenantId) {
        return MachineModel.builder().id(id).tenantId(tenantId).stationId(stationId)
                .machineCode("M1").name("M1").isActive(true).build();
    }

    private ComponentModel component(Long id, Long tenantId) {
        return ComponentModel.builder()
                .id(id).tenantId(tenantId).stationId(5L)
                .componentCode("C001").name("Comp1")
                .dataType(ComponentDataType.TELEMETRY).unit("°C")
                .reportIntervalSec(60)
                .normalUpper(BigDecimal.valueOf(100)).normalLower(BigDecimal.valueOf(0))
                .isActive(true).build();
    }

    // ── createStationComponent ───────────────────────────────────────────────────

    @Test
    void createStationComponent_happy_path() {
        given(stationRepo.findByIdAndTenantId(5L, 10L)).willReturn(Optional.of(station(5L, 10L)));
        given(componentRepo.existsByStationIdAndMachineIdIsNullAndComponentCode(5L, "C001")).willReturn(false);
        ComponentModel saved = component(1L, 10L);
        given(componentRepo.save(any())).willReturn(saved);

        ComponentModel result = service.createStationComponent(5L, 10L, "C001", "Comp1",
                ComponentDataType.TELEMETRY, "°C", 60, BigDecimal.TEN, BigDecimal.ZERO);

        assertThat(result).isSameAs(saved);
        then(componentRepo).should().save(argThat(c -> c.getMachineId() == null && c.getStationId() == 5L));
    }

    @Test
    void createStationComponent_throws_STATION_NOT_FOUND_when_station_missing() {
        given(stationRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStationComponent(99L, 10L, "C001", "n",
                ComponentDataType.EVENT, null, null, null, null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(STATION_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void createStationComponent_throws_COMPONENT_CODE_DUPLICATE_when_code_exists() {
        given(stationRepo.findByIdAndTenantId(5L, 10L)).willReturn(Optional.of(station(5L, 10L)));
        given(componentRepo.existsByStationIdAndMachineIdIsNullAndComponentCode(5L, "C001")).willReturn(true);

        assertThatThrownBy(() -> service.createStationComponent(5L, 10L, "C001", "n",
                ComponentDataType.EVENT, null, null, null, null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(COMPONENT_CODE_DUPLICATE.getCustomErrorCode());
    }

    // ── createMachineComponent ───────────────────────────────────────────────────

    @Test
    void createMachineComponent_happy_path() {
        MachineModel m = machine(20L, 5L, 10L);
        given(machineRepo.findByIdAndTenantId(20L, 10L)).willReturn(Optional.of(m));
        given(componentRepo.existsByMachineIdAndComponentCode(20L, "C001")).willReturn(false);
        ComponentModel saved = component(1L, 10L).toBuilder().machineId(20L).build();
        given(componentRepo.save(any())).willReturn(saved);

        ComponentModel result = service.createMachineComponent(20L, 10L, "C001", "Comp1",
                ComponentDataType.TELEMETRY, "°C", 60, BigDecimal.TEN, BigDecimal.ZERO);

        assertThat(result).isSameAs(saved);
        then(componentRepo).should().save(argThat(c -> Long.valueOf(20L).equals(c.getMachineId())));
    }

    @Test
    void createMachineComponent_throws_MACHINE_NOT_FOUND_when_machine_missing() {
        given(machineRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMachineComponent(99L, 10L, "C001", "n",
                ComponentDataType.EVENT, null, null, null, null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(MACHINE_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void createMachineComponent_throws_COMPONENT_CODE_DUPLICATE_when_code_exists() {
        given(machineRepo.findByIdAndTenantId(20L, 10L)).willReturn(Optional.of(machine(20L, 5L, 10L)));
        given(componentRepo.existsByMachineIdAndComponentCode(20L, "C001")).willReturn(true);

        assertThatThrownBy(() -> service.createMachineComponent(20L, 10L, "C001", "n",
                ComponentDataType.EVENT, null, null, null, null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(COMPONENT_CODE_DUPLICATE.getCustomErrorCode());
    }

    // ── updateComponent ──────────────────────────────────────────────────────────

    @Test
    void updateComponent_happy_path() {
        ComponentModel existing = component(1L, 10L);
        given(componentRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(existing));
        ComponentModel updated = existing.toBuilder().name("Updated").build();
        given(componentRepo.save(any())).willReturn(updated);

        ComponentModel result = service.updateComponent(1L, 10L, "Updated", "kPa", 30,
                BigDecimal.valueOf(200), BigDecimal.valueOf(-10));

        assertThat(result).isSameAs(updated);
    }

    @Test
    void updateComponent_throws_COMPONENT_NOT_FOUND_when_missing() {
        given(componentRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateComponent(99L, 10L, "n", null, null, null, null))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(COMPONENT_NOT_FOUND.getCustomErrorCode());
    }

    // ── deactivateComponent ──────────────────────────────────────────────────────

    @Test
    void deactivateComponent_happy_path() {
        ComponentModel existing = component(1L, 10L);
        given(componentRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(existing));

        service.deactivateComponent(1L, 10L);

        then(componentRepo).should().save(argThat(c -> !c.getIsActive()));
    }

    @Test
    void deactivateComponent_throws_COMPONENT_NOT_FOUND_when_missing() {
        given(componentRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateComponent(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(COMPONENT_NOT_FOUND.getCustomErrorCode());
    }

    // ── activateComponent ────────────────────────────────────────────────────────

    @Test
    void activateComponent_happy_path() {
        ComponentModel existing = component(1L, 10L).toBuilder().isActive(false).build();
        given(componentRepo.findByIdAndTenantId(1L, 10L)).willReturn(Optional.of(existing));
        ComponentModel activated = existing.toBuilder().isActive(true).build();
        given(componentRepo.save(any())).willReturn(activated);

        ComponentModel result = service.activateComponent(1L, 10L);

        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void activateComponent_throws_COMPONENT_NOT_FOUND_when_missing() {
        given(componentRepo.findByIdAndTenantId(99L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateComponent(99L, 10L))
                .isInstanceOf(IotCoreException.class)
                .extracting(e -> ((IotCoreException) e).getErrorCode())
                .isEqualTo(COMPONENT_NOT_FOUND.getCustomErrorCode());
    }
}
