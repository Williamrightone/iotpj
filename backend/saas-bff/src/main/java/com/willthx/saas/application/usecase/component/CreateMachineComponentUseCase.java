package com.willthx.saas.application.usecase.component;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.component.CreateComponentRq;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class CreateMachineComponentUseCase {

    private final IotCoreFeignClient iotCoreClient;

    public ComponentRs execute(Long machineId, CreateComponentRq rq) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() == Role.VIEWER) throw new SaasBffException(FORBIDDEN);
        return iotCoreClient.createMachineComponent(machineId, ctx.getTenantId(), rq).getData();
    }
}
