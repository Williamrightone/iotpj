package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.StationSummaryModel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StationSummaryRs {
    private Long    id;
    private String  stationCode;
    private String  name;
    private String  description;
    private Integer sortOrder;
    private Boolean isActive;
    private int     activeMachineCount;
    private int     activeComponentCount;

    public static StationSummaryRs from(StationSummaryModel m) {
        return StationSummaryRs.builder()
                .id(m.getStation().getId())
                .stationCode(m.getStation().getStationCode())
                .name(m.getStation().getName())
                .description(m.getStation().getDescription())
                .sortOrder(m.getStation().getSortOrder())
                .isActive(m.getStation().getIsActive())
                .activeMachineCount(m.getActiveMachineCount())
                .activeComponentCount(m.getActiveComponentCount())
                .build();
    }
}
