package com.willthx.uaa.domain.service.impl;

import com.willthx.uaa.domain.model.FeatureModel;
import com.willthx.uaa.domain.model.LoginResult;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.domain.port.TokenPort;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.domain.service.AuthService;
import com.willthx.uaa.domain.service.FeatureService;
import com.willthx.uaa.domain.service.JwtService;
import com.willthx.uaa.exception.UaaException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TokenPort      tokenPort;
    private final JwtService     jwtService;
    private final FeatureService featureService;

    @Value("${jwt.refresh-token-expiry-seconds:604800}")
    private long refreshTtl;

    @Override
    public LoginResult login(String account, String password) {
        // a. 驗證帳密（BCrypt 比對在 adapter 層）
        UserModel user = userRepository.authenticate(account, password)
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.INVALID_CREDENTIALS));

        // b. 帳號狀態
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UaaException(UaaException.UaaErrorType.ACCOUNT_DISABLED);
        }

        // c-e. 發行 Token
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getAccount(), user.getRole(), user.getTenantId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getTenantId());

        // f. 儲存 refresh token jti
        Claims refreshClaims = jwtService.parseToken(refreshToken);
        tokenPort.storeRefreshToken(refreshClaims.getId(), refreshTtl);

        // g. 更新最後登入時間
        userRepository.updateLastLogin(user.getId());

        // h-i. 站點快取
        List<String> stationIds = userRepository.findStationIds(user.getId(), user.getTenantId());
        tokenPort.cacheStations(user.getId(), stationIds, 3600L);

        // j. 功能清單
        List<FeatureModel> features = featureService.getFeatureTree(user.getTenantId(), user.getRole());

        return new LoginResult(accessToken, refreshToken, user, stationIds, features);
    }

    @Override
    public void logout(String accessJti, long accessRemainingSeconds, String refreshJti) {
        // a. 移除 refresh token
        tokenPort.removeRefreshToken(refreshJti);
        // b. 將 access token 加入黑名單
        if (accessRemainingSeconds > 0) {
            long expiryEpoch = System.currentTimeMillis() / 1000 + accessRemainingSeconds;
            tokenPort.blacklistToken(accessJti, expiryEpoch);
        }
    }

    @Override
    public String refresh(String refreshToken) {
        // a. 解析 refresh token
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken);
        } catch (JwtException e) {
            throw new UaaException(UaaException.UaaErrorType.REFRESH_TOKEN_NOT_FOUND);
        }

        // b. Redis 確認存活
        if (!tokenPort.existsRefreshToken(claims.getId())) {
            throw new UaaException(UaaException.UaaErrorType.REFRESH_TOKEN_NOT_FOUND);
        }

        Long userId   = Long.valueOf(claims.getSubject());
        Long tenantId = claims.get("tenantId", Long.class);

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new UaaException(UaaException.UaaErrorType.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UaaException(UaaException.UaaErrorType.ACCOUNT_DISABLED);
        }

        // c. 核發新 access token
        return jwtService.generateAccessToken(user.getId(), user.getAccount(), user.getRole(), tenantId);
    }
}
