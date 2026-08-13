package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GetRolePermissionsUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    GetRolePermissionsUseCase getRolePermissionsUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    @Test
    void execute_any_role_returns_permissions_map() {
        UserContext ctx = UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(Role.ADMIN).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
        UserContextHolder.set(ctx);

        Map<String, Object> data = Map.of("permissions", Map.of());
        @SuppressWarnings("unchecked")
        ApiResponse<Map<String, Object>> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(data);
        given(uaaClient.getRolePermissions(10L)).willReturn(response);

        Map<String, Object> result = getRolePermissionsUseCase.execute();

        assertThat(result).containsKey("permissions");
    }
}
