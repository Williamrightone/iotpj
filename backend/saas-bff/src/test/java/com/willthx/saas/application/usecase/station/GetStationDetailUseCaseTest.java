package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.station.StationDetailRs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class GetStationDetailUseCaseTest {

    @Mock IotCoreFeignClient iotCoreClient;
    @InjectMocks GetStationDetailUseCase useCase;

    @AfterEach void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctx(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_returns_station_detail_for_viewer() {
        UserContextHolder.set(ctx(Role.VIEWER));
        StationDetailRs rs = mock(StationDetailRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<StationDetailRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(iotCoreClient.getStationDetail(1L, 10L)).willReturn(response);

        assertThat(useCase.execute(1L)).isSameAs(rs);
    }
}
