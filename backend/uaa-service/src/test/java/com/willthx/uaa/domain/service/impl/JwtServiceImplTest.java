package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    static RSAPrivateKey privateKey;
    static RSAPublicKey  publicKey;
    static JwtServiceImpl jwtService;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) kp.getPrivate();
        publicKey  = (RSAPublicKey)  kp.getPublic();

        jwtService = new JwtServiceImpl(privateKey, publicKey);
        ReflectionTestUtils.setField(jwtService, "accessExpirySeconds",  900L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirySeconds", 604800L);
    }

    // ── generateAccessToken ───────────────────────────────────────────────────

    @Test
    void generate_access_token_claims_match_input() {
        String token = jwtService.generateAccessToken(1L, "user@test.com", Role.ADMIN, 10L);

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("account", String.class)).isEqualTo("user@test.com");
        assertThat(claims.get("role",    String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("tenantId", Long.class)).isEqualTo(10L);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generate_access_token_different_calls_produce_different_jti() {
        String t1 = jwtService.generateAccessToken(1L, "a@b.com", Role.VIEWER, 10L);
        String t2 = jwtService.generateAccessToken(1L, "a@b.com", Role.VIEWER, 10L);

        assertThat(jwtService.parseToken(t1).getId())
                .isNotEqualTo(jwtService.parseToken(t2).getId());
    }

    // ── generateRefreshToken ──────────────────────────────────────────────────

    @Test
    void generate_refresh_token_has_subject_and_tenantId() {
        String token = jwtService.generateRefreshToken(5L, 10L);

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("5");
        assertThat(claims.get("tenantId", Long.class)).isEqualTo(10L);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void generate_refresh_token_does_not_contain_account_or_role() {
        String token = jwtService.generateRefreshToken(5L, 10L);

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.get("account")).isNull();
        assertThat(claims.get("role")).isNull();
    }

    // ── parseToken ────────────────────────────────────────────────────────────

    @Test
    void parse_token_with_wrong_key_throws_JwtException() {
        // Generate a different key pair for tampered verification
        String token = jwtService.generateAccessToken(1L, "a@b.com", Role.ADMIN, 10L);

        // Tamper the token by changing the payload segment
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "TAMPER." + parts[2];

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parse_token_malformed_string_throws_JwtException() {
        assertThatThrownBy(() -> jwtService.parseToken("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    // ── buildJwks ─────────────────────────────────────────────────────────────

    @Test
    void build_jwks_contains_keys_array_with_rsa_entry() {
        Map<String, Object> jwks = jwtService.buildJwks();

        assertThat(jwks).containsKey("keys");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);

        Map<String, Object> jwk = keys.get(0);
        assertThat(jwk.get("kty")).isEqualTo("RSA");
        assertThat(jwk.get("use")).isEqualTo("sig");
        assertThat(jwk.get("alg")).isEqualTo("RS256");
        assertThat(jwk.get("kid")).isEqualTo("willthx-rs256");
        assertThat(jwk.get("n")).isNotNull().asString().isNotBlank();
        assertThat(jwk.get("e")).isNotNull().asString().isNotBlank();
    }
}
