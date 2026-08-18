package com.willthx.saas.application.api.dto.station;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderRq(
        @NotNull @Valid List<ReorderItemRq> orders
) {}
