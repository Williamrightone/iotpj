package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStationRq(
        @NotBlank @Size(max = 64)  String stationCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255)           String description
) {}
