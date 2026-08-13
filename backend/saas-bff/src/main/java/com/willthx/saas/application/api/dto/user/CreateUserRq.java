package com.willthx.saas.application.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateUserRq(@NotBlank @Email String account, @NotBlank String displayName,
                           @NotNull String role, @NotBlank String password,
                           List<String> stationIds) {}
