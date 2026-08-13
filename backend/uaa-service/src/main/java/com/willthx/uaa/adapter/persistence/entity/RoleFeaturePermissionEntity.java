package com.willthx.uaa.adapter.persistence.entity;

import com.willthx.common.model.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "role_feature_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_feature", columnNames = {"tenant_id", "role", "feature_id"})
)
@Getter @Setter @NoArgsConstructor
public class RoleFeaturePermissionEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;
}
