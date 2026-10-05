package com.skr1l.backend.warehouse.dto;

import jakarta.validation.constraints.NotBlank;

public record WarehouseRequestDTO(
        @NotBlank String name,
        @NotBlank String address
) {}
