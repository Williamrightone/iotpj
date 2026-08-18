package com.willthx.saas.application.api.dto.station;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.willthx.saas.application.api.dto.component.ComponentRs;
import com.willthx.saas.application.api.dto.machine.MachineDetailRs;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StationDetailRs {
    private StationRs              station;
    private List<ComponentRs>      stationComponents;
    private List<MachineDetailRs>  machines;
}
