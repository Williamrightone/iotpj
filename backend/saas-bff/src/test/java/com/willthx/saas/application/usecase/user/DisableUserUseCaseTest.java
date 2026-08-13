package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.exception.SaasBffException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.SELF_DISABLE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DisableUserUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    DisableUserUseCase disableUserUseCase;

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    private static UserContext ctxWith(Role role, Long userId) {
        return UserContext.builder()
                .userId(userId).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L)
                .stationIds(List.of()).build();
    }

    @Test
    void execute_admin_disables_other_user_calls_uaa() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));

        disableUserUseCase.execute(2L);

        verify(uaaClient).disableUser(2L, 10L);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.MAINTAINER, 1L));

        assertThatThrownBy(() -> disableUserUseCase.execute(2L))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }

    @Test
    void execute_admin_self_disable_throws_SaasBffException_SELF_DISABLE() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));

        assertThatThrownBy(() -> disableUserUseCase.execute(1L)) // same userId
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(SELF_DISABLE.getCustomErrorCode());
    }
}
