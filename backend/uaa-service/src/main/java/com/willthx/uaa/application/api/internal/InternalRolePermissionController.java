package com.willthx.uaa.application.api.internal;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.uaa.application.api.dto.feature.UpdateRolePermissionsRq;
import com.willthx.uaa.domain.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/role-permissions")
@RequiredArgsConstructor
public class InternalRolePermissionController {

    private final FeatureService featureService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRolePermissions(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(featureService.getRolePermissions(tenantId)));
    }

    @PutMapping("/{role}")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String role,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody UpdateRolePermissionsRq rq) {
        featureService.updateRolePermissions(tenantId, role, rq.featureIds());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
