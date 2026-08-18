package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.MachineModel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MachineRs {
    private Long          id;
    private Long          stationId;
    private String        machineCode;
    private String        name;
    private String        model;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MachineRs from(MachineModel m) {
        return MachineRs.builder()
                .id(m.getId())
                .stationId(m.getStationId())
                .machineCode(m.getMachineCode())
                .name(m.getName())
                .model(m.getModel())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
