package com.willthx.saas.application.api.dto.station;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StationSummaryRs {
    private Long    id;
    private String  stationCode;
    private String  name;
    private String  description;
    private Integer sortOrder;
    private Boolean isActive;
    private int     activeMachineCount;
    private int     activeComponentCount;
}
