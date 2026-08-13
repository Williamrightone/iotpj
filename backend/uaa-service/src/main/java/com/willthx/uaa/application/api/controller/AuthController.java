package com.willthx.uaa.application.api.controller;

import com.willthx.common.model.rest.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公開認證端點（/api/auth/*）。
 * uaa-service 直接暴露，對應 SPEC-1 中「saas-bff 直接轉送」的端點。
 * 實際登入邏輯在 InternalAuthController；此控制器保留給未來擴充（如 Health check）。
 *
 * <p>注意：demo 架構中 saas-bff 直接呼叫 /internal/auth/*，
 * 若需在 uaa 端也開放公網端點，可在此補充。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @org.springframework.web.bind.annotation.GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("uaa-service ok"));
    }
}
