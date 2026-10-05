package com.skr1l.backend.rack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RackRequestDto(
        @NotBlank String label,
        @NotNull Long warehouseId
) {}
