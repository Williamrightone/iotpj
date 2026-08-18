package com.willthx.saas.application.api.dto.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
}
