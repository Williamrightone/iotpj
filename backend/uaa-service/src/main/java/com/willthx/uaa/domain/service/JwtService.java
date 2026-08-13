package com.willthx.uaa.domain.service;

import com.willthx.common.model.enums.Role;
import io.jsonwebtoken.Claims;

import java.util.Map;

/**
 * JWT 簽發與驗證領域服務（Input Port）。
 */
public interface JwtService {

    /** 簽發 Access Token（RS256，15 min） */
    String generateAccessToken(Long userId, String account, Role role, Long tenantId);

    /** 簽發 Refresh Token（RS256，7 day），payload 僅含 userId / tenantId / jti */
    String generateRefreshToken(Long userId, Long tenantId);

    /** 解析並驗證 Token，回傳 Claims；驗簽失敗或過期拋例外 */
    Claims parseToken(String token);

    /** 回傳 JWKS Map（含公鑰），供 GET /internal/auth/jwks 使用 */
    Map<String, Object> buildJwks();
}
