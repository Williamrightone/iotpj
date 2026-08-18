package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContext;
import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.common.model.enums.Role;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.DeactivateResultRs;
import com.willthx.saas.exception.SaasBffException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.willthx.saas.exception.SaasBffException.SaasBffErrorType.FORBIDDEN;

@Component
@RequiredArgsConstructor
public class DeactivateStationUseCase {

    private final IotCoreFeignClient iotCoreClient;

    public DeactivateResultRs execute(Long id) {
        UserContext ctx = UserContextHolder.get();
        if (ctx.getRole() == Role.VIEWER) throw new SaasBffException(FORBIDDEN);
        return iotCoreClient.deactivateStation(id, ctx.getTenantId()).getData();
    }
}
