package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.ComponentModel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ComponentRs {
    private Long          id;
    private Long          stationId;
    private Long          machineId;
    private String        componentCode;
    private String        name;
    private String        dataType;
    private String        unit;
    private Integer       reportIntervalSec;
    private BigDecimal    normalUpper;
    private BigDecimal    normalLower;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ComponentRs from(ComponentModel m) {
        return ComponentRs.builder()
                .id(m.getId())
                .stationId(m.getStationId())
                .machineId(m.getMachineId())
                .componentCode(m.getComponentCode())
                .name(m.getName())
                .dataType(m.getDataType().name())
                .unit(m.getUnit())
                .reportIntervalSec(m.getReportIntervalSec())
                .normalUpper(m.getNormalUpper())
                .normalLower(m.getNormalLower())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
