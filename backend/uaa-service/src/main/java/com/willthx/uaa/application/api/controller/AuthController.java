package com.willthx.uaa.application.api.controller;

import com.willthx.uaa.domain.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認證 API 控制器。
 * 端點定義待 SDD 完成後實作。
 *
 * <p>預計端點（待確認）：
 * <ul>
 *   <li>POST /api/auth/login</li>
 *   <li>POST /api/auth/refresh</li>
 *   <li>POST /api/auth/logout</li>
 *   <li>GET  /internal/auth/jwks（供 saas-bff 取得公鑰）</li>
 * </ul>
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // TODO: 待 SDD 完成後定義端點
}
