package com.willthx.uaa.adapter.cache;

import com.willthx.uaa.domain.port.TokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * TokenPort 輸出埠的 Redis Adapter 實作。
 *
 * <p>Redis Key 規範：
 * <ul>
 *   <li>JWT 黑名單：{@code jwt:blacklist:{jti}}</li>
 *   <li>Refresh Token：{@code jwt:refresh:{userId}}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtCacheAdapter implements TokenPort {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REFRESH_PREFIX   = "jwt:refresh:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void blacklistToken(String jti, long expiryEpochSeconds) {
        long ttlSeconds = expiryEpochSeconds - Instant.now().getEpochSecond();
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue()
                    .set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    @Override
    public void storeRefreshToken(Long userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(REFRESH_PREFIX + userId, refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<String> getRefreshToken(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(REFRESH_PREFIX + userId));
    }

    @Override
    public void removeRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_PREFIX + userId);
    }
}
