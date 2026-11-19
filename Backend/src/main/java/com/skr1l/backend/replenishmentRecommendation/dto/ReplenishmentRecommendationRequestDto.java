package com.skr1l.backend.replenishmentRecommendation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ReplenishmentRecommendationRequestDto(
        @NotNull Long productId,
        @NotNull @Positive Integer recommendedQuantity,
        @NotNull @FutureOrPresent LocalDate requiredBy,
        @NotBlank String reason) {
}