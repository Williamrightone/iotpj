package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateComponentRq(
        @NotBlank @Size(max = 64)  String componentCode,
        @NotBlank @Size(max = 100) String name,
        @NotNull                   String dataType,
        @Size(max = 30)            String unit,
                                   Integer   reportIntervalSec,
                                   BigDecimal normalUpper,
                                   BigDecimal normalLower
) {
    @AssertTrue(message = "unit is required for TELEMETRY components")
    public boolean isUnitValidForDataType() {
        if ("TELEMETRY".equals(dataType)) {
            return unit != null && !unit.isBlank();
        }
        return true;
    }
}
