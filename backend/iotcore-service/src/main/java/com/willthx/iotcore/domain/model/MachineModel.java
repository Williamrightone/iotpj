package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class MachineModel {
    private final Long          id;
    private final Long          tenantId;
    private final Long          stationId;
    private final String        machineCode;
    private final String        name;
    private final String        model;
    private final Boolean       isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
