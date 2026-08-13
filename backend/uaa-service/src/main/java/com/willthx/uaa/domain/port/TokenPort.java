package com.willthx.uaa.domain.port;

import java.util.List;

/**
 * Token 與快取輸出埠（Output Port）。
 *
 * <p>Redis Key 規範：
 * <ul>
 *   <li>JWT 黑名單：{@code auth:blacklist:{jti}}</li>
 *   <li>Refresh Token：{@code auth:refresh:{jti}}</li>
 *   <li>站點快取：{@code user:stations:{userId}}</li>
 * </ul>
 */
public interface TokenPort {

    // ── Access Token 黑名單 ───────────────────────────────────────────────────

    void blacklistToken(String jti, long expiryEpochSeconds);

    boolean isBlacklisted(String jti);

    // ── Refresh Token ─────────────────────────────────────────────────────────

    void storeRefreshToken(String jti, long ttlSeconds);

    boolean existsRefreshToken(String jti);

    void removeRefreshToken(String jti);

    // ── 站點快取 ──────────────────────────────────────────────────────────────

    void cacheStations(Long userId, List<String> stationIds, long ttlSeconds);

    List<String> getCachedStations(Long userId);

    void evictStations(Long userId);
}
