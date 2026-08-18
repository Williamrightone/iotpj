package com.willthx.saas.application.usecase.station;

import com.willthx.common.model.auth.UserContextHolder;
import com.willthx.saas.adapter.feign.IotCoreFeignClient;
import com.willthx.saas.application.api.dto.station.StationSummaryRs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetStationListUseCase {

    private final IotCoreFeignClient iotCoreClient;

    public List<StationSummaryRs> execute() {
        Long tenantId = UserContextHolder.get().getTenantId();
        return iotCoreClient.listStations(tenantId).getData();
    }
}
