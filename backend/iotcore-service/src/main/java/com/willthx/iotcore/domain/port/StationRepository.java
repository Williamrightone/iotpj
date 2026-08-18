package com.willthx.iotcore.domain.port;

import com.willthx.iotcore.domain.model.StationModel;

import java.util.List;
import java.util.Optional;

public interface StationRepository {

    List<StationModel> findAllByTenantId(Long tenantId);

    Optional<StationModel> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndStationCode(Long tenantId, String stationCode);

    StationModel save(StationModel station);

    int maxSortOrderByTenantId(Long tenantId);

    void updateSortOrder(Long id, Long tenantId, int sortOrder);
}
