package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.user.UpdateStationsRq;
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
class UpdateStationsUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    UpdateStationsUseCase updateStationsUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctxWith(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_calls_uaa_updateStations() {
        UserContextHolder.set(ctxWith(Role.ADMIN));
        UpdateStationsRq rq = new UpdateStationsRq(List.of("ST-1", "ST-2"));

        updateStationsUseCase.execute(2L, rq);

        verify(uaaClient).updateStations(2L, 10L, rq);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.VIEWER));

        assertThatThrownBy(() -> updateStationsUseCase.execute(2L, new UpdateStationsRq(List.of())))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
