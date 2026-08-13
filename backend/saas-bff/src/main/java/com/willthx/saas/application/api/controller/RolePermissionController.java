package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.feature.UpdateRolePermissionsRq;
import com.willthx.saas.application.usecase.feature.GetRolePermissionsUseCase;
import com.willthx.saas.application.usecase.feature.UpdateRolePermissionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final GetRolePermissionsUseCase    getRolePermissionsUseCase;
    private final UpdateRolePermissionsUseCase updateRolePermissionsUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRolePermissions() {
        return ResponseEntity.ok(ApiResponse.success(getRolePermissionsUseCase.execute()));
    }

    @PutMapping("/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(@PathVariable String role,
                                                                   @RequestBody UpdateRolePermissionsRq rq) {
        updateRolePermissionsUseCase.execute(role, rq);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
