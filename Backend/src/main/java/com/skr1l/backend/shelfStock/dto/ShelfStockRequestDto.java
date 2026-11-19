package com.skr1l.backend.shelfStock.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ShelfStockRequestDto(

        @NotNull Long shelfId,

        @NotNull Long productId,

        @NotNull
        @Positive Integer quantity,

        @NotNull
        @FutureOrPresent LocalDate expirationDate) {
}