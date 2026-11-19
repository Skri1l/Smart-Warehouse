package com.skr1l.backend.stockMovement.dto;

import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import java.time.LocalDateTime;

public record StockMovementResponseDto(
        Long id,
        Long productId,
        Long shelfId,
        Integer quantityDelta,
        StockMovementType type,
        StockMovementSource source,
        LocalDateTime createdAt) {
}