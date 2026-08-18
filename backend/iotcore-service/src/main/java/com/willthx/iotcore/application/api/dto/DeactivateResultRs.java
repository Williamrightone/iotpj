package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.DeactivateResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeactivateResultRs {
    private int deactivatedMachines;
    private int deactivatedComponents;

    public static DeactivateResultRs from(DeactivateResult r) {
        return DeactivateResultRs.builder()
                .deactivatedMachines(r.getDeactivatedMachines())
                .deactivatedComponents(r.getDeactivatedComponents())
                .build();
    }
}
