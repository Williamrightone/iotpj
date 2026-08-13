package com.willthx.saas.application.usecase.feature;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.feature.UpdateRolePermissionsRq;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class UpdateRolePermissionsUseCase {

    private final UaaFeignClient uaaClient;

    public void execute(String role, UpdateRolePermissionsRq rq) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);
        uaaClient.updateRolePermissions(role, ctx.getTenantId(), rq);
    }
}
