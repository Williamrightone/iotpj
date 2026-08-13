package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.user.UpdateUserRq;
import com.willthx.saas.application.api.dto.user.UserRs;
import com.willthx.saas.exception.SaasBffException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.SELF_ROLE_CHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UpdateUserUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    UpdateUserUseCase updateUserUseCase;

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
    void execute_admin_updates_other_user_returns_result() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));
        UpdateUserRq rq = new UpdateUserRq("New Name", "VIEWER", List.of());
        UserRs rs = mock(UserRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<UserRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(uaaClient.updateUser(2L, 10L, rq)).willReturn(response);

        UserRs result = updateUserUseCase.execute(2L, rq);

        assertThat(result).isSameAs(rs);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.VIEWER, 1L));

        assertThatThrownBy(() -> updateUserUseCase.execute(2L, new UpdateUserRq("X", "VIEWER", List.of())))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }

    @Test
    void execute_admin_self_role_change_throws_SaasBffException_SELF_ROLE_CHANGE() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));
        // Same userId, role changed from ADMIN to VIEWER
        UpdateUserRq rq = new UpdateUserRq("Admin", "VIEWER", List.of());

        assertThatThrownBy(() -> updateUserUseCase.execute(1L, rq))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(SELF_ROLE_CHANGE.getCustomErrorCode());
    }

    @Test
    void execute_admin_self_update_same_role_succeeds() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));
        // Same userId, but role unchanged (ADMIN → ADMIN)
        UpdateUserRq rq = new UpdateUserRq("Updated Name", "ADMIN", List.of());
        UserRs rs = mock(UserRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<UserRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(uaaClient.updateUser(1L, 10L, rq)).willReturn(response);

        UserRs result = updateUserUseCase.execute(1L, rq);

        assertThat(result).isSameAs(rs);
    }
}
