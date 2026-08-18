package com.willthx.iotcore.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.iotcore.application.api.dto.ComponentRs;
import com.willthx.iotcore.application.api.dto.UpdateComponentRq;
import com.willthx.iotcore.domain.service.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/components")
@RequiredArgsConstructor
public class ComponentInternalController {

    private final ComponentService componentService;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ComponentRs>> updateComponent(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody UpdateComponentRq rq) {
        ComponentRs result = ComponentRs.from(
                componentService.updateComponent(id, tenantId,
                        rq.name(), rq.unit(), rq.reportIntervalSec(),
                        rq.normalUpper(), rq.normalLower()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateComponent(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        componentService.deactivateComponent(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ComponentRs>> activateComponent(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        ComponentRs result = ComponentRs.from(componentService.activateComponent(id, tenantId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
