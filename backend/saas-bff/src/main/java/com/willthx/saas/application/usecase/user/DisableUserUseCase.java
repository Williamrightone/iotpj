package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.SELF_DISABLE;

@Component
@RequiredArgsConstructor
public class DisableUserUseCase {

    private final UaaFeignClient uaaClient;

    public void execute(Long targetUserId) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);
        if (ctx.getUserId().equals(targetUserId)) throw new SaasBffException(SELF_DISABLE);
        uaaClient.disableUser(targetUserId, ctx.getTenantId());
    }
}
