package com.willthx.saas.application.api.dto.machine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineRs {
    private Long          id;
    private Long          stationId;
    private String        machineCode;
    private String        name;
    private String        model;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
