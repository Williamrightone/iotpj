package com.willthx.saas.application.usecase.auth;

import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.auth.RefreshRq;
import com.willthx.saas.application.api.dto.auth.RefreshRs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshUseCase {

    private final UaaFeignClient uaaClient;

    public RefreshRs execute(RefreshRq rq) {
        return uaaClient.refresh(rq).getData();
    }
}
