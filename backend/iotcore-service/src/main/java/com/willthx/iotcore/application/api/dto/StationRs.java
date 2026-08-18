package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.StationModel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StationRs {
    private Long          id;
    private String        stationCode;
    private String        name;
    private String        description;
    private Integer       sortOrder;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StationRs from(StationModel m) {
        return StationRs.builder()
                .id(m.getId())
                .stationCode(m.getStationCode())
                .name(m.getName())
                .description(m.getDescription())
                .sortOrder(m.getSortOrder())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
