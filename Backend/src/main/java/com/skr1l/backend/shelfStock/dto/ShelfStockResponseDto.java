package com.skr1l.backend.shelfStock.dto;

import com.skr1l.backend.shelfStock.enums.StockStatus;
import java.time.LocalDate;

public record ShelfStockResponseDto(
        Long id,
        Long shelfId,
        Long productId,
        Integer quantity,
        LocalDate expirationDate,
        StockStatus status) {
}