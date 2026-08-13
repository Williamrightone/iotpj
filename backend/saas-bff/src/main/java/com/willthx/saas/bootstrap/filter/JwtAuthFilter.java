package com.willthx.saas.bootstrap.filter;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String STATIONS_PREFIX  = "user:stations:";

    private final RSAPublicKey        jwtPublicKey;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(res, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            // 步驟 2+3：驗簽 + 過期自動拋例外
            claims = Jwts.parser()
                    .verifyWith(jwtPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            sendUnauthorized(res, "Invalid or expired token");
            return;
        }

        // 步驟 4：黑名單
        String jti = claims.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti))) {
            sendUnauthorized(res, "Token has been revoked");
            return;
        }

        // 步驟 5：解析 UserContext
        Long userId   = Long.valueOf(claims.getSubject());
        String account   = claims.get("account", String.class);
        Role   role      = Role.valueOf(claims.get("role", String.class));
        Long   tenantId  = claims.get("tenantId", Long.class);
        long   exp       = claims.getExpiration().getTime() / 1000;

        // 步驟 6：載入站點快取（MAINTAINER / VIEWER）
        List<String> stationIds = List.of();
        if (role == Role.MAINTAINER || role == Role.VIEWER) {
            String stationsJson = redisTemplate.opsForValue().get(STATIONS_PREFIX + userId);
            if (stationsJson != null && !stationsJson.isBlank()) {
                try {
                    stationIds = objectMapper.readValue(stationsJson,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (Exception e) {
                    log.warn("Failed to parse cached stations for userId={}", userId);
                }
            }
        }

        // 步驟 7-8
        UserContextHolder.set(UserContext.builder()
                .userId(userId)
                .account(account)
                .role(role)
                .tenantId(tenantId)
                .jti(jti)
                .exp(exp)
                .stationIds(stationIds)
                .build());

        try {
            chain.doFilter(req, res);
        } finally {
            UserContextHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getServletPath();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.startsWith("/actuator");
    }

    private void sendUnauthorized(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("UA00004", msg)));
    }
}
