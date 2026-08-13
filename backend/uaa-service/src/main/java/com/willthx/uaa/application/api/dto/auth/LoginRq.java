package com.willthx.uaa.application.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRq(
        @NotBlank String account,
        @NotBlank String password
) {}
