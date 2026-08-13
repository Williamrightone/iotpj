package com.willthx.uaa.application.api.internal;

import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.uaa.application.api.dto.user.*;
import com.willthx.uaa.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UaaUserRs>>> listUsers(
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        List<UaaUserRs> result = userService.listUsers(tenantId).stream()
                .map(UaaUserRs::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UaaUserRs>> getUser(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(UaaUserRs.from(userService.getUser(id, tenantId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UaaUserRs>> createUser(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody CreateUserRq rq) {
        var model = userService.createUser(tenantId, rq.account(), rq.displayName(),
                Role.valueOf(rq.role()), rq.password(), rq.stationIds());
        return ResponseEntity.ok(ApiResponse.success(UaaUserRs.from(model)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UaaUserRs>> updateUser(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @Valid @RequestBody UpdateUserRq rq) {
        var model = userService.updateUser(id, tenantId, rq.displayName(),
                Role.valueOf(rq.role()), rq.stationIds());
        return ResponseEntity.ok(ApiResponse.success(UaaUserRs.from(model)));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disableUser(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        userService.disableUser(id, tenantId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/stations")
    public ResponseEntity<ApiResponse<List<String>>> getStations(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getStations(id, tenantId)));
    }

    @PutMapping("/{id}/stations")
    public ResponseEntity<ApiResponse<Void>> updateStations(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody UpdateStationsRq rq) {
        userService.updateStations(id, tenantId, rq.stationIds());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
