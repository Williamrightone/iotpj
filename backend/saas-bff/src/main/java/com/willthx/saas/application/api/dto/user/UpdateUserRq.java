package com.willthx.saas.application.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRq(@NotBlank String displayName, @NotNull String role,
                           List<String> stationIds) {}
