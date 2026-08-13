package com.willthx.saas.application.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRq(@NotBlank String refreshToken) {}
