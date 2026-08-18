package com.willthx.saas.application.api.dto.machine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMachineRq(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100)           String model
) {}
