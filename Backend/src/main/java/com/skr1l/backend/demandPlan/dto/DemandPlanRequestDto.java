package com.skr1l.backend.demandPlan.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record DemandPlanRequestDto(
        @NotNull Long productId,
        @NotNull @FutureOrPresent LocalDate requiredDate,
        @NotNull @Positive Integer requiredQuantity) {
}