package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CopyMachineRq(
        @NotBlank @Size(max = 64)  String newMachineCode,
        @NotBlank @Size(max = 100) String newName
) {}
