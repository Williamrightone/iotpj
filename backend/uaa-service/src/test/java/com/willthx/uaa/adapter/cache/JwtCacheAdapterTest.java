package com.willthx.uaa.adapter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtCacheAdapterTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    JwtCacheAdapter adapter;
    ObjectMapper    objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter      = new JwtCacheAdapter(redisTemplate, objectMapper);
        // lenient: some tests don't use valueOps (hasKey / delete paths)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── blacklistToken ────────────────────────────────────────────────────────

    @Test
    void blacklist_token_future_expiry_stores_with_positive_ttl() {
        long futureEpoch = System.currentTimeMillis() / 1000 + 300;

        adapter.blacklistToken("jti-001", futureEpoch);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("auth:blacklist:jti-001"), eq("1"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue().getSeconds()).isGreaterThan(0).isLessThanOrEqualTo(300);
    }

    @Test
    void blacklist_token_past_expiry_does_not_call_redis() {
        long pastEpoch = System.currentTimeMillis() / 1000 - 10;

        adapter.blacklistToken("jti-old", pastEpoch);

        verify(valueOps, never()).set(any(), any(), any(Duration.class));
    }

    // ── isBlacklisted ─────────────────────────────────────────────────────────

    @Test
    void is_blacklisted_key_exists_returns_true() {
        given(redisTemplate.hasKey("auth:blacklist:jti-x")).willReturn(true);

        assertThat(adapter.isBlacklisted("jti-x")).isTrue();
    }

    @Test
    void is_blacklisted_key_absent_returns_false() {
        given(redisTemplate.hasKey("auth:blacklist:jti-y")).willReturn(false);

        assertThat(adapter.isBlacklisted("jti-y")).isFalse();
    }

    // ── storeRefreshToken ─────────────────────────────────────────────────────

    @Test
    void store_refresh_token_sets_value_with_correct_key_and_ttl() {
        adapter.storeRefreshToken("refresh-jti", 604800L);

        verify(valueOps).set("auth:refresh:refresh-jti", "1", Duration.ofSeconds(604800L));
    }

    // ── existsRefreshToken ────────────────────────────────────────────────────

    @Test
    void exists_refresh_token_key_exists_returns_true() {
        given(redisTemplate.hasKey("auth:refresh:jti-r")).willReturn(true);

        assertThat(adapter.existsRefreshToken("jti-r")).isTrue();
    }

    @Test
    void exists_refresh_token_key_absent_returns_false() {
        given(redisTemplate.hasKey("auth:refresh:jti-r")).willReturn(false);

        assertThat(adapter.existsRefreshToken("jti-r")).isFalse();
    }

    // ── removeRefreshToken ────────────────────────────────────────────────────

    @Test
    void remove_refresh_token_deletes_correct_key() {
        adapter.removeRefreshToken("jti-del");

        verify(redisTemplate).delete("auth:refresh:jti-del");
    }

    // ── cacheStations ─────────────────────────────────────────────────────────

    @Test
    void cache_stations_serializes_to_json_and_stores() throws Exception {
        List<String> stations = List.of("ST-1", "ST-2");

        adapter.cacheStations(1L, stations, 900L);

        String expectedJson = objectMapper.writeValueAsString(stations);
        verify(valueOps).set("user:stations:1", expectedJson, Duration.ofSeconds(900L));
    }

    // ── getCachedStations ─────────────────────────────────────────────────────

    @Test
    void get_cached_stations_key_absent_returns_empty_list() {
        given(valueOps.get("user:stations:2")).willReturn(null);

        assertThat(adapter.getCachedStations(2L)).isEmpty();
    }

    @Test
    void get_cached_stations_valid_json_returns_station_list() throws Exception {
        List<String> stations = List.of("ST-A", "ST-B");
        given(valueOps.get("user:stations:3")).willReturn(objectMapper.writeValueAsString(stations));

        assertThat(adapter.getCachedStations(3L)).containsExactly("ST-A", "ST-B");
    }

    @Test
    void get_cached_stations_invalid_json_returns_empty_list() {
        given(valueOps.get("user:stations:4")).willReturn("not-json");

        assertThat(adapter.getCachedStations(4L)).isEmpty();
    }

    // ── evictStations ─────────────────────────────────────────────────────────

    @Test
    void evict_stations_deletes_correct_key() {
        adapter.evictStations(5L);

        verify(redisTemplate).delete("user:stations:5");
    }
}
