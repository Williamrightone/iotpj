package com.willthx.uaa.adapter.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.willthx.uaa.domain.port.TokenPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCacheAdapter implements TokenPort {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX   = "auth:refresh:";
    private static final String STATIONS_PREFIX  = "user:stations:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    // ── Access Token 黑名單 ───────────────────────────────────────────────────

    @Override
    public void blacklistToken(String jti, long expiryEpochSeconds) {
        long ttl = expiryEpochSeconds - Instant.now().getEpochSecond();
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(ttl));
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Override
    public void storeRefreshToken(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean existsRefreshToken(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REFRESH_PREFIX + jti));
    }

    @Override
    public void removeRefreshToken(String jti) {
        redisTemplate.delete(REFRESH_PREFIX + jti);
    }

    // ── 站點快取 ──────────────────────────────────────────────────────────────

    @Override
    public void cacheStations(Long userId, List<String> stationIds, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(stationIds);
            redisTemplate.opsForValue().set(STATIONS_PREFIX + userId, json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to cache stations for userId={}", userId, e);
        }
    }

    @Override
    public List<String> getCachedStations(Long userId) {
        String json = redisTemplate.opsForValue().get(STATIONS_PREFIX + userId);
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to read cached stations for userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public void evictStations(Long userId) {
        redisTemplate.delete(STATIONS_PREFIX + userId);
    }
}
