package com.willthx.iotcore.application.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReorderItemRq(
        @NotNull Long id,
        @NotNull Integer sortOrder
) {}
