package com.willthx.iotcore.domain.model;

import com.willthx.common.model.enums.ComponentDataType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
public class ComponentModel {
    private final Long              id;
    private final Long              tenantId;
    private final Long              stationId;
    private final Long              machineId;
    private final String            componentCode;
    private final String            name;
    private final ComponentDataType dataType;
    private final String            unit;
    private final Integer           reportIntervalSec;
    private final BigDecimal        normalUpper;
    private final BigDecimal        normalLower;
    private final Boolean           isActive;
    private final LocalDateTime     createdAt;
    private final LocalDateTime     updatedAt;
}
