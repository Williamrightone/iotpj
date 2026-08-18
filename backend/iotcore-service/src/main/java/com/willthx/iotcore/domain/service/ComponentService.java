package com.willthx.iotcore.domain.service;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.iotcore.domain.model.ComponentModel;

import java.math.BigDecimal;

public interface ComponentService {

    ComponentModel createStationComponent(Long stationId, Long tenantId,
                                          String componentCode, String name, ComponentDataType dataType,
                                          String unit, Integer reportIntervalSec,
                                          BigDecimal normalUpper, BigDecimal normalLower);

    ComponentModel createMachineComponent(Long machineId, Long tenantId,
                                          String componentCode, String name, ComponentDataType dataType,
                                          String unit, Integer reportIntervalSec,
                                          BigDecimal normalUpper, BigDecimal normalLower);

    ComponentModel updateComponent(Long id, Long tenantId, String name,
                                   String unit, Integer reportIntervalSec,
                                   BigDecimal normalUpper, BigDecimal normalLower);

    void deactivateComponent(Long id, Long tenantId);

    ComponentModel activateComponent(Long id, Long tenantId);
}
