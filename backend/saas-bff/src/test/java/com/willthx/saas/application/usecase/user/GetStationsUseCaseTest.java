package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.rest.ApiResponse;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GetStationsUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    GetStationsUseCase getStationsUseCase;

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
    void execute_admin_can_query_any_user_stations() {
        UserContextHolder.set(ctxWith(Role.ADMIN, 1L));
        @SuppressWarnings("unchecked")
        ApiResponse<List<String>> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(List.of("ST-1", "ST-2"));
        given(uaaClient.getStations(99L, 10L)).willReturn(response);

        List<String> result = getStationsUseCase.execute(99L);

        assertThat(result).containsExactly("ST-1", "ST-2");
    }

    @Test
    void execute_viewer_queries_own_stations_succeeds() {
        UserContextHolder.set(ctxWith(Role.VIEWER, 5L));
        @SuppressWarnings("unchecked")
        ApiResponse<List<String>> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(List.of("ST-A"));
        given(uaaClient.getStations(5L, 10L)).willReturn(response);

        List<String> result = getStationsUseCase.execute(5L); // own userId

        assertThat(result).containsExactly("ST-A");
    }

    @Test
    void execute_viewer_queries_other_user_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.VIEWER, 5L));

        assertThatThrownBy(() -> getStationsUseCase.execute(99L)) // different userId
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
