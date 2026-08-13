package com.willthx.uaa.application.api.internal;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.uaa.application.api.dto.auth.*;
import com.willthx.uaa.application.api.dto.feature.UaaFeatureRs;
import com.willthx.uaa.domain.model.LoginResult;
import com.willthx.uaa.domain.service.AuthService;
import com.willthx.uaa.domain.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthService authService;
    private final JwtService  jwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UaaLoginRs>> login(@Valid @RequestBody LoginRq rq) {
        LoginResult result = authService.login(rq.account(), rq.password());
        UaaLoginRs rs = UaaLoginRs.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .userId(result.user().getId())
                .account(result.user().getAccount())
                .displayName(result.user().getDisplayName())
                .role(result.user().getRole().name())
                .tenantId(result.user().getTenantId())
                .stationIds(result.stationIds())
                .features(result.features().stream().map(UaaFeatureRs::from).collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(ApiResponse.success(rs));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRq rq) {
        authService.logout(rq.accessJti(), rq.accessRemainingSeconds(), rq.refreshJti());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UaaRefreshRs>> refresh(@Valid @RequestBody RefreshRq rq) {
        String newAccessToken = authService.refresh(rq.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(new UaaRefreshRs(newAccessToken)));
    }

    @GetMapping("/jwks")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(jwtService.buildJwks());
    }
}
