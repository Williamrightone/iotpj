package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetRolePermissionsUseCase {

    private final UaaFeignClient uaaClient;

    public Map<String, Object> execute() {
        Long tenantId = UserContextHolder.get().getTenantId();
        return uaaClient.getRolePermissions(tenantId).getData();
    }
}
