package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetFeaturesUseCase {

    private final UaaFeignClient uaaClient;

    public List<FeatureRs> execute() {
        Long tenantId = UserContextHolder.get().getTenantId();
        return uaaClient.listFeatures(tenantId).getData();
    }
}
