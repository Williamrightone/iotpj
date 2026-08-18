package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeactivateResult {
    private final int deactivatedMachines;
    private final int deactivatedComponents;
}
