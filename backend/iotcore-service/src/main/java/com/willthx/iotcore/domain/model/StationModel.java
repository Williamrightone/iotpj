package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class StationModel {
    private final Long          id;
    private final Long          tenantId;
    private final String        stationCode;
    private final String        name;
    private final String        description;
    private final Integer       sortOrder;
    private final Boolean       isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
