package com.willthx.saas.application.api.dto.feature;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureRs {
    private Long    featureId;
    private String  featureCode;
    private String  featureName;
    private Long    parentId;
    private String  route;
    private int     sortOrder;
    private boolean isActive;
}
