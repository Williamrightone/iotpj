package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.station.CreateStationRq;
import com.willthx.saas.application.api.dto.station.StationRs;
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
class CreateStationUseCaseTest {

    @Mock IotCoreFeignClient iotCoreClient;
    @InjectMocks CreateStationUseCase useCase;

    @AfterEach void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctx(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_creates_station() {
        UserContextHolder.set(ctx(Role.ADMIN));
        CreateStationRq rq = new CreateStationRq("ST001", "Station 1", null);
        StationRs rs = mock(StationRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<StationRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(iotCoreClient.createStation(10L, rq)).willReturn(response);

        assertThat(useCase.execute(rq)).isSameAs(rs);
    }

    @Test
    void execute_maintainer_creates_station() {
        UserContextHolder.set(ctx(Role.MAINTAINER));
        CreateStationRq rq = new CreateStationRq("ST001", "Station 1", null);
        StationRs rs = mock(StationRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<StationRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(iotCoreClient.createStation(10L, rq)).willReturn(response);

        assertThat(useCase.execute(rq)).isSameAs(rs);
    }

    @Test
    void execute_viewer_throws_FORBIDDEN() {
        UserContextHolder.set(ctx(Role.VIEWER));
        assertThatThrownBy(() -> useCase.execute(new CreateStationRq("ST001", "S1", null)))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
