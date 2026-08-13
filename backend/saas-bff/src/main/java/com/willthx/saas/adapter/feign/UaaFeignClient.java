package com.willthx.saas.adapter.feign;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.auth.LoginRq;
import com.willthx.saas.application.api.dto.auth.LoginRs;
import com.willthx.saas.application.api.dto.auth.RefreshRq;
import com.willthx.saas.application.api.dto.auth.RefreshRs;
import com.willthx.saas.application.api.dto.feature.CreateFeatureRq;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import com.willthx.saas.application.api.dto.feature.UpdateActiveRq;
import com.willthx.saas.application.api.dto.feature.UpdateFeatureRq;
import com.willthx.saas.application.api.dto.feature.UpdateRolePermissionsRq;
import com.willthx.saas.application.api.dto.user.CreateUserRq;
import com.willthx.saas.application.api.dto.user.UpdateStationsRq;
import com.willthx.saas.application.api.dto.user.UpdateUserRq;
import com.willthx.saas.application.api.dto.user.UserRs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "uaa-service", url = "${uaa.service.url:http://localhost:8081}")
public interface UaaFeignClient {

    // ── Auth ─────────────────────────────────────────────────────────────────

    @PostMapping("/internal/auth/login")
    ApiResponse<LoginRs> login(@RequestBody LoginRq rq);

    @PostMapping("/internal/auth/logout")
    ApiResponse<Void> logout(@RequestBody UaaLogoutPayload rq);

    @PostMapping("/internal/auth/refresh")
    ApiResponse<RefreshRs> refresh(@RequestBody RefreshRq rq);

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/internal/users")
    ApiResponse<List<UserRs>> listUsers(@RequestHeader("X-Tenant-Id") Long tenantId);

    @GetMapping("/internal/users/{id}")
    ApiResponse<UserRs> getUser(@PathVariable("id") Long id,
                                @RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/users")
    ApiResponse<UserRs> createUser(@RequestHeader("X-Tenant-Id") Long tenantId,
                                   @RequestBody CreateUserRq rq);

    @PutMapping("/internal/users/{id}")
    ApiResponse<UserRs> updateUser(@PathVariable("id") Long id,
                                   @RequestHeader("X-Tenant-Id") Long tenantId,
                                   @RequestBody UpdateUserRq rq);

    @PostMapping("/internal/users/{id}/disable")
    ApiResponse<Void> disableUser(@PathVariable("id") Long id,
                                  @RequestHeader("X-Tenant-Id") Long tenantId);

    @GetMapping("/internal/users/{id}/stations")
    ApiResponse<List<String>> getStations(@PathVariable("id") Long id,
                                          @RequestHeader("X-Tenant-Id") Long tenantId);

    @PutMapping("/internal/users/{id}/stations")
    ApiResponse<Void> updateStations(@PathVariable("id") Long id,
                                     @RequestHeader("X-Tenant-Id") Long tenantId,
                                     @RequestBody UpdateStationsRq rq);

    // ── Features ──────────────────────────────────────────────────────────────

    @GetMapping("/internal/features")
    ApiResponse<List<FeatureRs>> listFeatures(@RequestHeader("X-Tenant-Id") Long tenantId);

    @PostMapping("/internal/features")
    ApiResponse<FeatureRs> createFeature(@RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody CreateFeatureRq rq);

    @PutMapping("/internal/features/{id}")
    ApiResponse<FeatureRs> updateFeature(@PathVariable("id") Long id,
                                         @RequestHeader("X-Tenant-Id") Long tenantId,
                                         @RequestBody UpdateFeatureRq rq);

    @PutMapping("/internal/features/{id}/active")
    ApiResponse<Void> setActive(@PathVariable("id") Long id,
                                @RequestHeader("X-Tenant-Id") Long tenantId,
                                @RequestBody UpdateActiveRq rq);

    @DeleteMapping("/internal/features/{id}")
    ApiResponse<Void> deleteFeature(@PathVariable("id") Long id,
                                    @RequestHeader("X-Tenant-Id") Long tenantId);

    // ── Role Permissions ──────────────────────────────────────────────────────

    @GetMapping("/internal/role-permissions")
    ApiResponse<Map<String, Object>> getRolePermissions(@RequestHeader("X-Tenant-Id") Long tenantId);

    @PutMapping("/internal/role-permissions/{role}")
    ApiResponse<Void> updateRolePermissions(@PathVariable("role") String role,
                                            @RequestHeader("X-Tenant-Id") Long tenantId,
                                            @RequestBody UpdateRolePermissionsRq rq);
}
