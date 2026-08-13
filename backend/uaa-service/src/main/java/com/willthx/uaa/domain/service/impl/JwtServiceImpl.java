package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final RSAPrivateKey jwtPrivateKey;
    private final RSAPublicKey  jwtPublicKey;

    @Value("${jwt.access-token-expiry-seconds:900}")
    private long accessExpirySeconds;

    @Value("${jwt.refresh-token-expiry-seconds:604800}")
    private long refreshExpirySeconds;

    @Override
    public String generateAccessToken(Long userId, String account, Role role, Long tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("account",  account)
                .claim("role",     role.name())
                .claim("tenantId", tenantId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpirySeconds)))
                .signWith(jwtPrivateKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, Long tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("tenantId", tenantId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpirySeconds)))
                .signWith(jwtPrivateKey)
                .compact();
    }

    @Override
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public Map<String, Object> buildJwks() {
        byte[] nBytes = jwtPublicKey.getModulus().toByteArray();
        if (nBytes[0] == 0) {
            nBytes = Arrays.copyOfRange(nBytes, 1, nBytes.length);
        }
        byte[] eBytes = jwtPublicKey.getPublicExponent().toByteArray();

        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        Map<String, Object> jwk = new LinkedHashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", "willthx-rs256");
        jwk.put("n",   enc.encodeToString(nBytes));
        jwk.put("e",   enc.encodeToString(eBytes));

        return Map.of("keys", List.of(jwk));
    }
}
