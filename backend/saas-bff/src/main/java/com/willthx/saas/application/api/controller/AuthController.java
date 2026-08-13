package com.willthx.saas.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.application.api.dto.auth.LoginRq;
import com.willthx.saas.application.api.dto.auth.LoginRs;
import com.willthx.saas.application.api.dto.auth.LogoutRq;
import com.willthx.saas.application.api.dto.auth.RefreshRq;
import com.willthx.saas.application.api.dto.auth.RefreshRs;
import com.willthx.saas.application.usecase.auth.LoginUseCase;
import com.willthx.saas.application.usecase.auth.LogoutUseCase;
import com.willthx.saas.application.usecase.auth.RefreshUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase   loginUseCase;
    private final LogoutUseCase  logoutUseCase;
    private final RefreshUseCase refreshUseCase;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginRs>> login(@Valid @RequestBody LoginRq rq) {
        return ResponseEntity.ok(ApiResponse.success(loginUseCase.execute(rq)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRq rq) {
        logoutUseCase.execute(rq);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshRs>> refresh(@Valid @RequestBody RefreshRq rq) {
        return ResponseEntity.ok(ApiResponse.success(refreshUseCase.execute(rq)));
    }
}
