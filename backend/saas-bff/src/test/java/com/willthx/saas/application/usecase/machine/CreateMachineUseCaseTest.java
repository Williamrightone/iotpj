package com.willthx.saas.application.usecase.machine;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.machine.CreateMachineRq;
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
class CreateMachineUseCaseTest {

    @Mock IotCoreFeignClient iotCoreClient;
    @InjectMocks CreateMachineUseCase useCase;

    @AfterEach void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctx(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_creates_machine() {
        UserContextHolder.set(ctx(Role.ADMIN));
        CreateMachineRq rq = new CreateMachineRq("M001", "Machine 1", "ModelX");
        MachineRs rs = mock(MachineRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<MachineRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(iotCoreClient.createMachine(5L, 10L, rq)).willReturn(response);

        assertThat(useCase.execute(5L, rq)).isSameAs(rs);
    }

    @Test
    void execute_viewer_throws_FORBIDDEN() {
        UserContextHolder.set(ctx(Role.VIEWER));
        assertThatThrownBy(() -> useCase.execute(5L, new CreateMachineRq("M001", "M1", null)))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
