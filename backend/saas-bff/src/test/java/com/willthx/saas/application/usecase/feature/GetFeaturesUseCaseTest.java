package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class GetFeaturesUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    GetFeaturesUseCase getFeaturesUseCase;

    @AfterEach
    void clearContext() { UserContextHolder.clear(); }

    @Test
    void execute_any_role_returns_feature_list() {
        UserContext ctx = UserContext.builder().userId(1L).account("a@b.com").displayName("A")
                .role(Role.VIEWER).tenantId(10L).jti("jti").exp(9999999999L).stationIds(List.of()).build();
        UserContextHolder.set(ctx);

        FeatureRs rs = mock(FeatureRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<List<FeatureRs>> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(List.of(rs));
        given(uaaClient.listFeatures(10L)).willReturn(response);

        List<FeatureRs> result = getFeaturesUseCase.execute();

        assertThat(result).hasSize(1).first().isSameAs(rs);
    }
}
