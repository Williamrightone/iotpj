package com.willthx.uaa.application.api.internal;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.uaa.application.api.dto.feature.*;
import com.willthx.uaa.domain.service.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/features")
@RequiredArgsConstructor
public class InternalFeatureController {

    private final FeatureService featureService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UaaFeatureRs>>> listFeatures(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        List<UaaFeatureRs> result = featureService.listFeatures(tenantId).stream()
                .map(UaaFeatureRs::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UaaFeatureRs>> createFeature(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateFeatureRq rq) {
        var model = featureService.createFeature(tenantId, rq.parentId(), rq.featureCode(),
                rq.featureName(), rq.route(), rq.sortOrder());
        return ResponseEntity.ok(ApiResponse.success(UaaFeatureRs.from(model)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UaaFeatureRs>> updateFeature(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody UpdateFeatureRq rq) {
        var model = featureService.updateFeature(id, tenantId, rq.featureName(), rq.route(), rq.sortOrder());
        return ResponseEntity.ok(ApiResponse.success(UaaFeatureRs.from(model)));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Void>> setActive(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody UpdateActiveRq rq) {
        featureService.setActive(id, tenantId, rq.active());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        featureService.deleteFeature(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** 依 role 回傳過濾後的功能平坦清單（登入時使用） */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<UaaFeatureRs>>> featureTree(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader("X-User-Role")  String role) {
        List<UaaFeatureRs> result = featureService.getFeatureTree(tenantId, Role.valueOf(role)).stream()
                .map(UaaFeatureRs::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
