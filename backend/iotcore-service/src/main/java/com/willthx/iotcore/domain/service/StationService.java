package com.willthx.iotcore.domain.service;

import com.willthx.iotcore.domain.model.DeactivateResult;
import com.willthx.iotcore.domain.model.ReorderEntry;
import com.willthx.iotcore.domain.model.StationDetailModel;
import com.willthx.iotcore.domain.model.StationModel;
import com.willthx.iotcore.domain.model.StationSummaryModel;

import java.util.List;

public interface StationService {

    List<StationSummaryModel> listStations(Long tenantId);

    StationModel createStation(Long tenantId, String stationCode, String name, String description);

    StationModel updateStation(Long id, Long tenantId, String name, String description);

    void reorderStations(Long tenantId, List<ReorderEntry> orders);

    DeactivateResult deactivateStation(Long id, Long tenantId);

    StationModel activateStation(Long id, Long tenantId);

    StationDetailModel getStationDetail(Long id, Long tenantId);
}
