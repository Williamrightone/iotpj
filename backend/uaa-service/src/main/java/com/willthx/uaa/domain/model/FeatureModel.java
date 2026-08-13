package com.willthx.uaa.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeatureModel {
    private final Long          id;
    private final Long          tenantId;
    private final Long          parentId;
    private final String        featureCode;
    private final String        featureName;
    private final String        route;
    private final int           sortOrder;
    private final boolean       active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
