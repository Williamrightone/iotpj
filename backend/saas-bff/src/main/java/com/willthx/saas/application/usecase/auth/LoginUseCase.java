package com.willthx.saas.application.usecase.auth;

import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.auth.LoginRq;
import com.willthx.saas.application.api.dto.auth.LoginRs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginUseCase {

    private final UaaFeignClient uaaClient;

    public LoginRs execute(LoginRq rq) {
        return uaaClient.login(rq).getData();
    }
}
