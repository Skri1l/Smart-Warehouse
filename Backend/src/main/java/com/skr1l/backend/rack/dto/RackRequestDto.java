package com.skr1l.backend.rack.dto;

import jakarta.validation.constraints.NotBlank;

public record RackRequestDto(
        @NotBlank String label,
        @NotBlank Long warehouseId
) {}
