package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.UpdateActiveRq;
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
class SetFeatureActiveUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    SetFeatureActiveUseCase setFeatureActiveUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctxWith(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_calls_uaa_setActive() {
        UserContextHolder.set(ctxWith(Role.ADMIN));
        UpdateActiveRq rq = new UpdateActiveRq(false);

        setFeatureActiveUseCase.execute(100L, rq);

        verify(uaaClient).setActive(100L, 10L, rq);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.VIEWER));

        assertThatThrownBy(() -> setFeatureActiveUseCase.execute(100L, new UpdateActiveRq(true)))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
