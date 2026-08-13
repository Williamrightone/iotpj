package com.willthx.uaa.application.api.dto.feature;

import jakarta.validation.constraints.NotBlank;

public record CreateFeatureRq(
        Long    parentId,
        @NotBlank String featureCode,
        @NotBlank String featureName,
        String  route,
        int     sortOrder
) {}
