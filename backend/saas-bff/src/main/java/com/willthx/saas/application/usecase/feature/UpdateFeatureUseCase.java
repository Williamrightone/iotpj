package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.FeatureRs;
import com.willthx.saas.application.api.dto.feature.UpdateFeatureRq;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class UpdateFeatureUseCase {

    private final UaaFeignClient uaaClient;

    public FeatureRs execute(Long featureId, UpdateFeatureRq rq) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);
        return uaaClient.updateFeature(featureId, ctx.getTenantId(), rq).getData();
    }
}
