package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.station.StationDetailRs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetStationDetailUseCase {

    private final IotCoreFeignClient iotCoreClient;

    public StationDetailRs execute(Long id) {
        Long tenantId = UserContextHolder.get().getTenantId();
        return iotCoreClient.getStationDetail(id, tenantId).getData();
    }
}
