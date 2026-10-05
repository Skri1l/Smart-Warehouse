package com.skr1l.backend.purchaseOrder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseOrderRequestDto(
        @NotNull Long recommendationId,
        @NotNull @Positive Integer quantity) {
}