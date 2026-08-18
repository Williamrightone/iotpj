package com.willthx.saas.application.api.dto.station;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StationRs {
    private Long          id;
    private String        stationCode;
    private String        name;
    private String        description;
    private Integer       sortOrder;
    private Boolean       isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
