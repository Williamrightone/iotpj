package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.component.UpdateComponentRq;
import com.willthx.saas.application.usecase.component.ActivateComponentUseCase;
import com.willthx.saas.application.usecase.component.DeactivateComponentUseCase;
import com.willthx.saas.application.usecase.component.UpdateComponentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {

    private final UpdateComponentUseCase    updateComponentUseCase;
    private final DeactivateComponentUseCase deactivateComponentUseCase;
    private final ActivateComponentUseCase  activateComponentUseCase;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ComponentRs>> updateComponent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateComponentRq rq) {
        return ResponseEntity.ok(ApiResponse.success(updateComponentUseCase.execute(id, rq)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateComponent(@PathVariable Long id) {
        deactivateComponentUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ComponentRs>> activateComponent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(activateComponentUseCase.execute(id)));
    }
}
