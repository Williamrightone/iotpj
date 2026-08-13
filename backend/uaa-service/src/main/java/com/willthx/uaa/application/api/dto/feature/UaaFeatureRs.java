package com.willthx.uaa.application.api.dto.feature;

import com.willthx.uaa.domain.model.FeatureModel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UaaFeatureRs {
    private Long    featureId;
    private String  featureCode;
    private String  featureName;
    private Long    parentId;
    private String  route;
    private int     sortOrder;
    private boolean isActive;

    public static UaaFeatureRs from(FeatureModel m) {
        return UaaFeatureRs.builder()
                .featureId(m.getId())
                .featureCode(m.getFeatureCode())
                .featureName(m.getFeatureName())
                .parentId(m.getParentId())
                .route(m.getRoute())
                .sortOrder(m.getSortOrder())
                .isActive(m.isActive())
                .build();
    }
}
