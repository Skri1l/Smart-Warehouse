package com.skr1l.backend.stockMovement.dto;

import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import jakarta.validation.constraints.NotNull;

public record StockMovementRequestDto(

        @NotNull Long productId,

        @NotNull Long shelfId,

        @NotNull Integer quantityDelta,

        @NotNull StockMovementType type,

        @NotNull StockMovementSource source) {
}