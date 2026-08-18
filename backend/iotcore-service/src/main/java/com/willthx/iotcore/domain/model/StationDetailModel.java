package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StationDetailModel {
    private final StationModel           station;
    private final List<ComponentModel>   stationComponents;
    private final List<MachineDetailModel> machines;
}
