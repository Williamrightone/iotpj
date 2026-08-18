package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.DeactivateResultRs;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.component.CreateComponentRq;
import com.willthx.saas.application.api.dto.machine.CopyMachineRq;
import com.willthx.saas.application.api.dto.machine.MachineRs;
import com.willthx.saas.application.api.dto.machine.UpdateMachineRq;
import com.willthx.saas.application.usecase.component.CreateMachineComponentUseCase;
import com.willthx.saas.application.usecase.machine.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final UpdateMachineUseCase          updateMachineUseCase;
    private final DeactivateMachineUseCase      deactivateMachineUseCase;
    private final ActivateMachineUseCase        activateMachineUseCase;
    private final CopyMachineUseCase            copyMachineUseCase;
    private final CreateMachineComponentUseCase createMachineComponentUseCase;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MachineRs>> updateMachine(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMachineRq rq) {
        return ResponseEntity.ok(ApiResponse.success(updateMachineUseCase.execute(id, rq)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DeactivateResultRs>> deactivateMachine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(deactivateMachineUseCase.execute(id)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MachineRs>> activateMachine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(activateMachineUseCase.execute(id)));
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<ApiResponse<MachineRs>> copyMachine(
            @PathVariable Long id,
            @Valid @RequestBody CopyMachineRq rq) {
        return ResponseEntity.ok(ApiResponse.success(copyMachineUseCase.execute(id, rq)));
    }

    @PostMapping("/{machineId}/components")
    public ResponseEntity<ApiResponse<ComponentRs>> createMachineComponent(
            @PathVariable Long machineId,
            @Valid @RequestBody CreateComponentRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createMachineComponentUseCase.execute(machineId, rq)));
    }
}
