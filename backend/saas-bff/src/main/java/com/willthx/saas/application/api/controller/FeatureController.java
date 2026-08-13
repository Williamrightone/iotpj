package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.feature.CreateFeatureRq;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import com.willthx.saas.application.api.dto.feature.UpdateActiveRq;
import com.willthx.saas.application.api.dto.feature.UpdateFeatureRq;
import com.willthx.saas.application.usecase.feature.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final GetFeaturesUseCase    getFeaturesUseCase;
    private final CreateFeatureUseCase  createFeatureUseCase;
    private final UpdateFeatureUseCase  updateFeatureUseCase;
    private final SetFeatureActiveUseCase setFeatureActiveUseCase;
    private final DeleteFeatureUseCase  deleteFeatureUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeatureRs>>> listFeatures() {
        return ResponseEntity.ok(ApiResponse.success(getFeaturesUseCase.execute()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeatureRs>> createFeature(@Valid @RequestBody CreateFeatureRq rq) {
        return ResponseEntity.ok(ApiResponse.success(createFeatureUseCase.execute(rq)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeatureRs>> updateFeature(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateFeatureRq rq) {
        return ResponseEntity.ok(ApiResponse.success(updateFeatureUseCase.execute(id, rq)));
    }

    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setActive(@PathVariable Long id,
                                                       @RequestBody UpdateActiveRq rq) {
        setFeatureActiveUseCase.execute(id, rq);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFeature(@PathVariable Long id) {
        deleteFeatureUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
