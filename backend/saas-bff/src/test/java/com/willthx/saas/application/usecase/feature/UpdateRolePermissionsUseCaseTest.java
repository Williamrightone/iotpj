package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.UpdateRolePermissionsRq;
import com.willthx.saas.exception.SaasBffException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateRolePermissionsUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    UpdateRolePermissionsUseCase updateRolePermissionsUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctxWith(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_calls_uaa_updateRolePermissions() {
        UserContextHolder.set(ctxWith(Role.ADMIN));
        UpdateRolePermissionsRq rq = new UpdateRolePermissionsRq(List.of(100L, 200L));

        updateRolePermissionsUseCase.execute("VIEWER", rq);

        verify(uaaClient).updateRolePermissions("VIEWER", 10L, rq);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.MAINTAINER));

        assertThatThrownBy(() -> updateRolePermissionsUseCase.execute("VIEWER",
                new UpdateRolePermissionsRq(List.of())))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
