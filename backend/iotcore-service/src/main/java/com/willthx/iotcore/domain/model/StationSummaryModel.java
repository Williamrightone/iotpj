package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationSummaryModel {
    private final StationModel station;
    private final int          activeMachineCount;
    private final int          activeComponentCount;
}
