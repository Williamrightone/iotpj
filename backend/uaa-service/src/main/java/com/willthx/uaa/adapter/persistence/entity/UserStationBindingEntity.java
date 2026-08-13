package com.willthx.uaa.adapter.persistence.entity;

import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_station_bindings",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_station", columnNames = {"user_id", "station_id"})
)
@Getter @Setter @NoArgsConstructor
public class UserStationBindingEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "station_id", nullable = false, length = 64)
    private String stationId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
