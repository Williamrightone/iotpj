package com.willthx.saas.application.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRq(@NotBlank String refreshToken) {}
