package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.user.CreateUserRq;
import com.willthx.saas.application.api.dto.user.UserRs;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UaaFeignClient uaaClient;

    public UserRs execute(CreateUserRq rq) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);
        return uaaClient.createUser(ctx.getTenantId(), rq).getData();
    }
}
