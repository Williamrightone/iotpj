package com.willthx.saas.application.usecase.auth;

import com.willthx.common.model.rest.ApiResponse;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.auth.LoginRq;
import com.willthx.saas.application.api.dto.auth.LoginRs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock UaaFeignClient uaaClient;

    @InjectMocks
    LoginUseCase loginUseCase;

    @Test
    void execute_delegates_to_uaa_and_returns_login_result() {
        LoginRq rq = new LoginRq("user@example.com", "secret");
        LoginRs rs = mock(LoginRs.class);
        @SuppressWarnings("unchecked")
        ApiResponse<LoginRs> response = mock(ApiResponse.class);
        given(response.getData()).willReturn(rs);
        given(uaaClient.login(rq)).willReturn(response);

        LoginRs result = loginUseCase.execute(rq);

        assertThat(result).isSameAs(rs);
    }
}
