package com.willthx.iotcore.application.api.dto;

import com.willthx.iotcore.domain.model.MachineDetailModel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MachineDetailRs {
    private Long               id;
    private Long               stationId;
    private String             machineCode;
    private String             name;
    private String             model;
    private Boolean            isActive;
    private List<ComponentRs>  components;

    public static MachineDetailRs from(MachineDetailModel m) {
        return MachineDetailRs.builder()
                .id(m.getMachine().getId())
                .stationId(m.getMachine().getStationId())
                .machineCode(m.getMachine().getMachineCode())
                .name(m.getMachine().getName())
                .model(m.getMachine().getModel())
                .isActive(m.getMachine().getIsActive())
                .components(m.getComponents().stream().map(ComponentRs::from).toList())
                .build();
    }
}
