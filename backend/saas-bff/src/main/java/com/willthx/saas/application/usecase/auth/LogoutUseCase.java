package com.willthx.saas.application.usecase.auth;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.adapter.feign.UaaLogoutPayload;
import com.willthx.saas.application.api.dto.auth.LogoutRq;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoutUseCase {

    private final UaaFeignClient uaaClient;
    private final RSAPublicKey   jwtPublicKey;

    public void execute(LogoutRq rq) {
        UserContext ctx = UserContextHolder.get();
        String accessJti             = ctx.getJti();
        long   accessRemainingSeconds = Math.max(0L, ctx.getExp() - (System.currentTimeMillis() / 1000));

        String refreshJti = parseRefreshJti(rq.refreshToken());
        uaaClient.logout(new UaaLogoutPayload(accessJti, accessRemainingSeconds, refreshJti));
    }

    private String parseRefreshJti(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtPublicKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
            return claims.getId();
        } catch (JwtException e) {
            log.warn("Invalid refresh token during logout: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }
}
