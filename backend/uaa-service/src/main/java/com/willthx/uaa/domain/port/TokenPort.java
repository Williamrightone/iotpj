package com.willthx.uaa.domain.port;

import java.util.Optional;

/**
 * Token 快取輸出埠（Output Port）。
 * Adapter 層（JwtCacheAdapter）以 Redis 實作此介面。
 */
public interface TokenPort {

    /**
     * 將 jti 加入黑名單（登出時呼叫）。
     * TTL = Access Token 剩餘有效秒數。
     */
    void blacklistToken(String jti, long expiryEpochSeconds);

    /** 檢查 jti 是否在黑名單中 */
    boolean isBlacklisted(String jti);

    /**
     * 儲存 Refresh Token。
     *
     * @param userId       使用者 ID
     * @param refreshToken Refresh Token 字串
     * @param ttlSeconds   存活時間（秒）
     */
    void storeRefreshToken(Long userId, String refreshToken, long ttlSeconds);

    /** 取得 Refresh Token（不存在或已過期回傳 empty） */
    Optional<String> getRefreshToken(Long userId);

    /** 移除 Refresh Token（登出時呼叫） */
    void removeRefreshToken(Long userId);
}
