package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateComponentRq(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 30)            String unit,
                                   Integer   reportIntervalSec,
                                   BigDecimal normalUpper,
                                   BigDecimal normalLower
) {}
