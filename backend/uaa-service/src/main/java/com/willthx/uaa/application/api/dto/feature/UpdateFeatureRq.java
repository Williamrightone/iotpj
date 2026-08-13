package com.willthx.uaa.application.api.dto.feature;

import jakarta.validation.constraints.NotBlank;

public record UpdateFeatureRq(
        @NotBlank String featureName,
        String  route,
        int     sortOrder
) {}
