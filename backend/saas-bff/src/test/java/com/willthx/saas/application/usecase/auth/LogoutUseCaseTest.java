package com.willthx.saas.application.usecase.auth;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.adapter.feign.UaaLogoutPayload;
import com.willthx.saas.application.api.dto.auth.LogoutRq;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    static RSAPrivateKey privateKey;
    static RSAPublicKey  publicKey;
    static LogoutUseCase logoutUseCase;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey  = (RSAPublicKey)  kp.getPublic();
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    private LogoutUseCase buildUseCase() {
        return new LogoutUseCase(uaaClient, publicKey);
    }

    private String buildRefreshToken(String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject("1")
                .claim("tenantId", 10L)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(604800)))
                .signWith(privateKey)
                .compact();
    }

    private UserContext buildContext(long expEpochSeconds) {
        return UserContext.builder()
                .userId(1L).account("user@test.com").displayName("Test")
                .role(Role.ADMIN).tenantId(10L)
                .jti("access-jti-001")
                .exp(expEpochSeconds)
                .stationIds(List.of())
                .build();
    }

    // ── execute ───────────────────────────────────────────────────────────────

    @Test
    void execute_valid_token_calls_uaa_logout_with_correct_payload() {
        String refreshJti   = UUID.randomUUID().toString();
        String refreshToken = buildRefreshToken(refreshJti);
        long   futureExp    = System.currentTimeMillis() / 1000 + 300;

        UserContextHolder.set(buildContext(futureExp));

        buildUseCase().execute(new LogoutRq(refreshToken));

        ArgumentCaptor<UaaLogoutPayload> captor = ArgumentCaptor.forClass(UaaLogoutPayload.class);
        verify(uaaClient).logout(captor.capture());

        UaaLogoutPayload payload = captor.getValue();
        assertThat(payload.accessJti()).isEqualTo("access-jti-001");
        assertThat(payload.accessRemainingSeconds()).isGreaterThan(0);
        assertThat(payload.refreshJti()).isEqualTo(refreshJti);
    }

    @Test
    void execute_expired_access_token_sends_zero_remaining_seconds() {
        String refreshToken = buildRefreshToken(UUID.randomUUID().toString());
        long   pastExp      = System.currentTimeMillis() / 1000 - 60; // already expired

        UserContextHolder.set(buildContext(pastExp));

        buildUseCase().execute(new LogoutRq(refreshToken));

        ArgumentCaptor<UaaLogoutPayload> captor = ArgumentCaptor.forClass(UaaLogoutPayload.class);
        verify(uaaClient).logout(captor.capture());
        assertThat(captor.getValue().accessRemainingSeconds()).isEqualTo(0L);
    }

    @Test
    void execute_invalid_refresh_token_throws_IllegalArgumentException() {
        UserContextHolder.set(buildContext(System.currentTimeMillis() / 1000 + 300));

        assertThatThrownBy(() -> buildUseCase().execute(new LogoutRq("not.a.jwt.token")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}
