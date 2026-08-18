package com.willthx.saas.application.api.dto.station;

import jakarta.validation.constraints.NotNull;

public record ReorderItemRq(
        @NotNull Long    id,
        @NotNull Integer sortOrder
) {}
