package com.willthx.saas.application.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeactivateResultRs {
    private int deactivatedMachines;
    private int deactivatedComponents;
}
