package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.station.ReorderItemRq;
import com.willthx.saas.application.api.dto.station.ReorderRq;
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
class ReorderStationsUseCaseTest {

    @Mock IotCoreFeignClient iotCoreClient;
    @InjectMocks ReorderStationsUseCase useCase;

    @AfterEach void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctx(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_reorders_stations_for_admin() {
        UserContextHolder.set(ctx(Role.ADMIN));
        ReorderRq rq = new ReorderRq(List.of(new ReorderItemRq(1L, 2), new ReorderItemRq(2L, 1)));

        useCase.execute(rq);

        then(iotCoreClient).should().reorderStations(10L, rq);
    }

    @Test
    void execute_viewer_throws_FORBIDDEN() {
        UserContextHolder.set(ctx(Role.VIEWER));
        assertThatThrownBy(() -> useCase.execute(new ReorderRq(List.of())))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
