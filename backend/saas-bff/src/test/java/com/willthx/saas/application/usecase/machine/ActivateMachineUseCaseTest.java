package com.willthx.saas.application.usecase.machine;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.machine.MachineRs;
import com.willthx.saas.exception.SaasBffException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateMachineUseCaseTest {

    @Mock IotCoreFeignClient iotCoreClient;
    @InjectMocks ActivateMachineUseCase useCase;

    @AfterEach void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctx(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_activates_machine_for_admin() {
        UserContextHolder.set(ctx(Role.ADMIN));
        MachineRs rs = mock(MachineRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<MachineRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(iotCoreClient.activateMachine(1L, 10L)).willReturn(response);

        assertThat(useCase.execute(1L)).isSameAs(rs);
    }

    @Test
    void execute_viewer_throws_FORBIDDEN() {
        UserContextHolder.set(ctx(Role.VIEWER));
        assertThatThrownBy(() -> useCase.execute(1L))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
