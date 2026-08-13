package com.willthx.uaa.adapter.persistence.entity;

import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "features",
        uniqueConstraints = @UniqueConstraint(name = "uq_features_code", columnNames = {"tenant_id", "feature_code"})
)
@Getter @Setter @NoArgsConstructor
public class FeatureEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "feature_code", nullable = false, length = 100)
    private String featureCode;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Column(name = "route", length = 255)
    private String route;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
