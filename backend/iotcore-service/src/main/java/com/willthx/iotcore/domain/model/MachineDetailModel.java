package com.willthx.iotcore.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MachineDetailModel {
    private final MachineModel         machine;
    private final List<ComponentModel> components;
}
