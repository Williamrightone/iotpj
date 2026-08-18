package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMachineRq(
        @NotBlank @Size(max = 64)  String machineCode,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100)           String model
) {}
