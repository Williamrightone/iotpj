package com.willthx.iotcore.adapter.persistence.entity;

import com.willthx.common.model.enums.ComponentDataType;
import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "iot_components")
@Getter
@Setter
public class IotComponentEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private ComponentDataType dataType;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "report_interval_sec")
    private Integer reportIntervalSec;

    @Column(name = "normal_upper", precision = 15, scale = 4)
    private BigDecimal normalUpper;

    @Column(name = "normal_lower", precision = 15, scale = 4)
    private BigDecimal normalLower;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
