package com.willthx.uaa.application.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRq(
        @NotBlank String accessJti,
        long             accessRemainingSeconds,
        @NotBlank String refreshJti
) {}
