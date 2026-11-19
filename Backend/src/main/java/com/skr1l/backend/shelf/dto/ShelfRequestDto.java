package com.skr1l.backend.shelf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ShelfRequestDto(
        @NotBlank String label,

        @NotNull
        @Positive Integer capacity,

        @NotNull Long rackId) {
}
