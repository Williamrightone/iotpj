package com.willthx.iotcore.application.api.controller;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.iotcore.application.api.dto.*;
import com.willthx.iotcore.domain.model.ReorderEntry;
import com.willthx.iotcore.domain.service.ComponentService;
import com.willthx.iotcore.domain.service.MachineService;
import com.willthx.iotcore.domain.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/stations")
@RequiredArgsConstructor
public class StationInternalController {

    private final StationService   stationService;
    private final MachineService   machineService;
    private final ComponentService componentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StationSummaryRs>>> listStations(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        List<StationSummaryRs> result = stationService.listStations(tenantId).stream()
                .map(StationSummaryRs::from).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StationRs>> createStation(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateStationRq rq) {
        StationRs result = StationRs.from(
                stationService.createStation(tenantId, rq.stationCode(), rq.name(), rq.description()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StationRs>> updateStation(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody UpdateStationRq rq) {
        StationRs result = StationRs.from(
                stationService.updateStation(id, tenantId, rq.name(), rq.description()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderStations(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody ReorderRq rq) {
        List<ReorderEntry> orders = rq.orders().stream()
                .map(item -> new ReorderEntry(item.id(), item.sortOrder()))
                .toList();
        stationService.reorderStations(tenantId, orders);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DeactivateResultRs>> deactivateStation(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        DeactivateResultRs result = DeactivateResultRs.from(
                stationService.deactivateStation(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<StationRs>> activateStation(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        StationRs result = StationRs.from(stationService.activateStation(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<StationDetailRs>> getStationDetail(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        StationDetailRs result = StationDetailRs.from(stationService.getStationDetail(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{stationId}/machines")
    public ResponseEntity<ApiResponse<MachineRs>> createMachine(
            @PathVariable Long stationId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateMachineRq rq) {
        MachineRs result = MachineRs.from(
                machineService.createMachine(stationId, tenantId, rq.machineCode(), rq.name(), rq.model()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{stationId}/components")
    public ResponseEntity<ApiResponse<ComponentRs>> createStationComponent(
            @PathVariable Long stationId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateComponentRq rq) {
        ComponentRs result = ComponentRs.from(componentService.createStationComponent(
                stationId, tenantId,
                rq.componentCode(), rq.name(),
                ComponentDataType.valueOf(rq.dataType()),
                rq.unit(), rq.reportIntervalSec(), rq.normalUpper(), rq.normalLower()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
