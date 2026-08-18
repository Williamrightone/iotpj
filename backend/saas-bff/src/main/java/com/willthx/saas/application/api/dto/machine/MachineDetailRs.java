package com.willthx.saas.application.api.dto.machine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineDetailRs {
    private Long              id;
    private Long              stationId;
    private String            machineCode;
    private String            name;
    private String            model;
    private Boolean           isActive;
    private List<ComponentRs> components;
}
