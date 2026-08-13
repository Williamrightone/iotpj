package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.user.UpdateUserRq;
import com.willthx.saas.application.api.dto.user.UserRs;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;
import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.SELF_ROLE_CHANGE;

@Component
@RequiredArgsConstructor
public class UpdateUserUseCase {

    private final UaaFeignClient uaaClient;

    public UserRs execute(Long targetUserId, UpdateUserRq rq) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);

        // 不得修改自己的角色
        if (ctx.getUserId().equals(targetUserId)
                && !ctx.getRole().name().equals(rq.role())) {
            throw new SaasBffException(SELF_ROLE_CHANGE);
        }

        return uaaClient.updateUser(targetUserId, ctx.getTenantId(), rq).getData();
    }
}
