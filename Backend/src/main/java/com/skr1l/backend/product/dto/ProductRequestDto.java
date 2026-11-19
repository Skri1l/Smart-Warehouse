package com.skr1l.backend.product.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductRequestDto(

        @NotBlank String sku,

        @NotBlank String name,

        String description) {
}