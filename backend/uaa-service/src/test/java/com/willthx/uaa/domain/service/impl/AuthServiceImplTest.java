package com.willthx.uaa.domain.service.impl;

import com.willthx.common.model.enums.Role;
import com.willthx.uaa.domain.model.FeatureModel;
import com.willthx.uaa.domain.model.LoginResult;
import com.willthx.uaa.domain.model.UserModel;
import com.willthx.uaa.domain.model.UserStatus;
import com.willthx.uaa.domain.port.TokenPort;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.domain.service.FeatureService;
import com.willthx.uaa.domain.service.JwtService;
import com.willthx.uaa.exception.UaaException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.willthx.uaa.exception.UaaException.UaaErrorType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository  userRepository;
    @Mock TokenPort       tokenPort;
    @Mock JwtService      jwtService;
    @Mock FeatureService  featureService;

    @InjectMocks
    AuthServiceImpl authService;

    private UserModel activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTtl", 604800L);

        activeUser = UserModel.builder()
                .id(1L)
                .account("user@example.com")
                .displayName("Test User")
                .role(Role.ADMIN)
                .tenantId(10L)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_valid_credentials_returns_LoginResult() {
        Claims claims = mock(Claims.class);
        given(claims.getId()).willReturn("refresh-jti-001");
        given(userRepository.authenticate("user@example.com", "secret")).willReturn(Optional.of(activeUser));
        given(jwtService.generateAccessToken(anyLong(), anyString(), any(), anyLong())).willReturn("access.tok.en");
        given(jwtService.generateRefreshToken(anyLong(), anyLong())).willReturn("refresh.tok.en");
        given(jwtService.parseToken("refresh.tok.en")).willReturn(claims);
        given(userRepository.findStationIds(1L, 10L)).willReturn(List.of("ST-A", "ST-B"));
        given(featureService.getFeatureTree(10L, Role.ADMIN)).willReturn(List.of());

        LoginResult result = authService.login("user@example.com", "secret");

        assertThat(result.accessToken()).isEqualTo("access.tok.en");
        assertThat(result.refreshToken()).isEqualTo("refresh.tok.en");
        assertThat(result.user().getAccount()).isEqualTo("user@example.com");
        assertThat(result.stationIds()).containsExactly("ST-A", "ST-B");
        verify(tokenPort).storeRefreshToken("refresh-jti-001", 604800L);
        verify(userRepository).updateLastLogin(1L);
        verify(tokenPort).cacheStations(eq(1L), eq(List.of("ST-A", "ST-B")), anyLong());
    }

    @Test
    void login_wrong_password_throws_UaaException_INVALID_CREDENTIALS() {
        given(userRepository.authenticate("user@example.com", "wrong")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("user@example.com", "wrong"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(INVALID_CREDENTIALS.getCustomErrorCode());
    }

    @Test
    void login_account_disabled_throws_UaaException_ACCOUNT_DISABLED() {
        UserModel disabled = UserModel.builder()
                .id(2L).account("dis@example.com").role(Role.VIEWER)
                .tenantId(10L).status(UserStatus.DISABLED).build();
        given(userRepository.authenticate("dis@example.com", "pwd")).willReturn(Optional.of(disabled));

        assertThatThrownBy(() -> authService.login("dis@example.com", "pwd"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(ACCOUNT_DISABLED.getCustomErrorCode());
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_positive_remaining_seconds_blacklists_access_token() {
        authService.logout("access-jti", 300L, "refresh-jti");

        verify(tokenPort).removeRefreshToken("refresh-jti");
        verify(tokenPort).blacklistToken(eq("access-jti"), anyLong());
    }

    @Test
    void logout_zero_remaining_seconds_does_not_blacklist_access_token() {
        authService.logout("access-jti", 0L, "refresh-jti");

        verify(tokenPort).removeRefreshToken("refresh-jti");
        verify(tokenPort, never()).blacklistToken(any(), anyLong());
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_valid_token_returns_new_access_token() {
        Claims claims = mock(Claims.class);
        given(claims.getId()).willReturn("refresh-jti");
        given(claims.getSubject()).willReturn("1");
        given(claims.get("tenantId", Long.class)).willReturn(10L);
        given(jwtService.parseToken("refresh.tok")).willReturn(claims);
        given(tokenPort.existsRefreshToken("refresh-jti")).willReturn(true);
        given(userRepository.findById(1L)).willReturn(Optional.of(activeUser));
        given(jwtService.generateAccessToken(1L, "user@example.com", Role.ADMIN, 10L)).willReturn("new.access.tok");

        String result = authService.refresh("refresh.tok");

        assertThat(result).isEqualTo("new.access.tok");
    }

    @Test
    void refresh_token_not_in_redis_throws_UaaException_REFRESH_TOKEN_NOT_FOUND() {
        Claims claims = mock(Claims.class);
        given(claims.getId()).willReturn("refresh-jti");
        given(jwtService.parseToken("refresh.tok")).willReturn(claims);
        given(tokenPort.existsRefreshToken("refresh-jti")).willReturn(false);

        assertThatThrownBy(() -> authService.refresh("refresh.tok"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(REFRESH_TOKEN_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void refresh_invalid_jwt_throws_UaaException_REFRESH_TOKEN_NOT_FOUND() {
        given(jwtService.parseToken("bad.token")).willThrow(new JwtException("expired"));

        assertThatThrownBy(() -> authService.refresh("bad.token"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(REFRESH_TOKEN_NOT_FOUND.getCustomErrorCode());
    }

    @Test
    void refresh_user_disabled_throws_UaaException_ACCOUNT_DISABLED() {
        Claims claims = mock(Claims.class);
        given(claims.getId()).willReturn("jti");
        given(claims.getSubject()).willReturn("2");
        given(claims.get("tenantId", Long.class)).willReturn(10L);
        given(jwtService.parseToken("refresh.tok")).willReturn(claims);
        given(tokenPort.existsRefreshToken("jti")).willReturn(true);
        UserModel disabled = UserModel.builder().id(2L).account("d@x.com").role(Role.VIEWER)
                .tenantId(10L).status(UserStatus.DISABLED).build();
        given(userRepository.findById(2L)).willReturn(Optional.of(disabled));

        assertThatThrownBy(() -> authService.refresh("refresh.tok"))
                .isInstanceOf(UaaException.class)
                .extracting(e -> ((UaaException) e).getErrorCode())
                .isEqualTo(ACCOUNT_DISABLED.getCustomErrorCode());
    }
}
