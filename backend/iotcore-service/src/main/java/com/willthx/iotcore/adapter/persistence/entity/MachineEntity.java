package com.willthx.iotcore.adapter.persistence.entity;

import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "machines",
    uniqueConstraints = @UniqueConstraint(name = "uq_machine_code", columnNames = {"tenant_id", "machine_code"})
)
@Getter
@Setter
public class MachineEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "machine_code", nullable = false, length = 64)
    private String machineCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
