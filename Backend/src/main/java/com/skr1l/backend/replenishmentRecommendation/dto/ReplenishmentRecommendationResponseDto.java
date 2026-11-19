package com.skr1l.backend.replenishmentRecommendation.dto;

import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReplenishmentRecommendationResponseDto(
        Long id,
        Long productId,
        Integer recommendedQuantity,
        LocalDate requiredBy,
        String reason,
        RecommendationStatus status,
        LocalDateTime createdAt) {
}