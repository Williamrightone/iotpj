package com.willthx.saas.application.usecase.auth;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.auth.RefreshRq;
import com.willthx.saas.application.api.dto.auth.RefreshRs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RefreshUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    RefreshUseCase refreshUseCase;

    @Test
    void execute_delegates_to_uaa_and_returns_refresh_result() {
        RefreshRq rq = new RefreshRq("refresh.tok.en");
        RefreshRs rs = mock(RefreshRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<RefreshRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(uaaClient.refresh(rq)).willReturn(response);

        RefreshRs result = refreshUseCase.execute(rq);

        assertThat(result).isSameAs(rs);
    }
}
