package com.willthx.iotcore.adapter.persistence.entity;

import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "stations",
    uniqueConstraints = @UniqueConstraint(name = "uq_station_code", columnNames = {"tenant_id", "station_code"})
)
@Getter
@Setter
public class StationEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "station_code", nullable = false, length = 64)
    private String stationCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
