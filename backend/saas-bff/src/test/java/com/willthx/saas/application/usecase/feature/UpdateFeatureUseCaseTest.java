package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import com.willthx.saas.application.api.dto.feature.UpdateFeatureRq;
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
class UpdateFeatureUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    UpdateFeatureUseCase updateFeatureUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    private static UserContext ctxWith(Role role) {
        return UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(role).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
    }

    @Test
    void execute_admin_updates_feature_and_returns_result() {
        UserContextHolder.set(ctxWith(Role.ADMIN));
        UpdateFeatureRq rq = new UpdateFeatureRq("新名稱", "/new", 1);
        FeatureRs rs = mock(FeatureRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<FeatureRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(uaaClient.updateFeature(100L, 10L, rq)).willReturn(response);

        assertThat(updateFeatureUseCase.execute(100L, rq)).isSameAs(rs);
    }

    @Test
    void execute_non_admin_throws_SaasBffException_FORBIDDEN() {
        UserContextHolder.set(ctxWith(Role.VIEWER));

        assertThatThrownBy(() -> updateFeatureUseCase.execute(100L, new UpdateFeatureRq("X", null, 0)))
                .isInstanceOf(SaasBffException.class)
                .extracting(e -> ((SaasBffException) e).getErrorCode())
                .isEqualTo(FORBIDDEN.getCustomErrorCode());
    }
}
