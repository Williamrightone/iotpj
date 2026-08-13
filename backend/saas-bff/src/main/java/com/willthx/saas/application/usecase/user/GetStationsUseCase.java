package com.willthx.saas.application.usecase.user;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.UaaFeignClient;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class GetStationsUseCase {

    private final UaaFeignClient uaaClient;

    public List<String> execute(Long targetUserId) {
        UserContext ctx = UserContextHolder.get();
        // ADMIN 可查任意使用者；其他角色只能查自己
        if (ctx.getRole() != Role.ADMIN && !ctx.getUserId().equals(targetUserId)) {
            throw new SaasBffException(FORBIDDEN);
        }
        return uaaClient.getStations(targetUserId, ctx.getTenantId()).getData();
    }
}
