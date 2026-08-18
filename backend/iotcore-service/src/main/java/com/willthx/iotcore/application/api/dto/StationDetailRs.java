package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.StationDetailModel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StationDetailRs {
    private StationRs          station;
    private List<ComponentRs>  stationComponents;
    private List<MachineDetailRs> machines;

    public static StationDetailRs from(StationDetailModel m) {
        return StationDetailRs.builder()
                .station(StationRs.from(m.getStation()))
                .stationComponents(m.getStationComponents().stream().map(ComponentRs::from).toList())
                .machines(m.getMachines().stream().map(MachineDetailRs::from).toList())
                .build();
    }
}
