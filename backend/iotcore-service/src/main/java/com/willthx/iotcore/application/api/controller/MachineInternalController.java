package com.willthx.iotcore.application.api.controller;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.iotcore.application.api.dto.*;
import com.willthx.iotcore.domain.service.ComponentService;
import com.willthx.iotcore.domain.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/machines")
@RequiredArgsConstructor
public class MachineInternalController {

    private final MachineService   machineService;
    private final ComponentService componentService;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineRs>> updateMachine(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody UpdateMachineRq rq) {
        MachineRs result = MachineRs.from(
                machineService.updateMachine(id, tenantId, rq.name(), rq.model()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DeactivateResultRs>> deactivateMachine(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        DeactivateResultRs result = DeactivateResultRs.from(
                machineService.deactivateMachine(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MachineRs>> activateMachine(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        MachineRs result = MachineRs.from(machineService.activateMachine(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<MachineRs>> copyMachine(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CopyMachineRq rq) {
        MachineRs result = MachineRs.from(
                machineService.copyMachine(id, tenantId, rq.newMachineCode(), rq.newName()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{machineId}/components")
    public ResponseEntity<ApiResponse<ComponentRs>> createMachineComponent(
            @PathVariable Long machineId,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateComponentRq rq) {
        ComponentRs result = ComponentRs.from(componentService.createMachineComponent(
                machineId, tenantId,
                rq.componentCode(), rq.name(),
                ComponentDataType.valueOf(rq.dataType()),
                rq.unit(), rq.reportIntervalSec(), rq.normalUpper(), rq.normalLower()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
