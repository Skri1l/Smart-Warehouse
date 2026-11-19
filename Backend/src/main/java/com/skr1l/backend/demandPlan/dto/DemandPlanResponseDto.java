package com.skr1l.backend.demandPlan.dto;

import java.time.LocalDate;

public record DemandPlanResponseDto(
        Long id,
        Long productId,
        LocalDate requiredDate,
        Integer requiredQuantity) {
}