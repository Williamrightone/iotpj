package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.application.api.dto.user.UserRs;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class GetUsersUseCase {

    private final UaaFeignClient uaaClient;

    public List<UserRs> execute() {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() != Role.ADMIN) throw new SaasBffException(FORBIDDEN);
        return uaaClient.listUsers(ctx.getTenantId()).getData();
    }
}
